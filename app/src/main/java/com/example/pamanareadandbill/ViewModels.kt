package com.example.pamanareadandbill

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.String

/**
 * Shared interface for ViewModels that handle data imports.
 */
interface ResettableImport {
    fun resetImportResult()
}

class CustomerViewModel : ViewModel() {
    var customers by mutableStateOf<List<CustomerInfo>>(emptyList())
        private set
    var history by mutableStateOf<List<ReadHistory>>(emptyList())
        private set
    var findings by mutableStateOf<List<FieldFindings>>(emptyList())
        private set
    var waterRates by mutableStateOf<List<WaterRates>>(emptyList())
    var acctRates: WaterRates? = null
    var currentIndex by mutableStateOf(0)
        private set

    // Add UI State for the reading form
    var reading by mutableStateOf("")
    var consumption by mutableStateOf("")
    var pesoValue by mutableStateOf("")
    var selectedFinding by mutableStateOf("                ")
    var remarks by mutableStateOf("")
    var averageLast3 by mutableStateOf(0)
    var isModified by mutableStateOf(false)

    // Add a function to update the reading
    fun updateReading(value: String) {
        reading = value
        isModified = true
    }
    fun updateConsumption(value: String) {
        consumption = value
        isModified = true
    }
    fun updateValue(value: String) { pesoValue = value }
    fun updateSelectedFinding(value: String) {
        selectedFinding = value
        isModified = true
    }
    fun updateRemarks(value: String) {
        remarks = value
        isModified = true
    }
    fun updateAverageLast3(value: Int) { averageLast3 = value }
    fun moveTo(value: Int) { currentIndex = value}
    fun resetModified() { isModified = false }

    fun loadCustomers(db: AppDatabase) {
        viewModelScope.launch {
            customers = db.customerDao().getCustomers()
            if(customers.isNotEmpty()) {
                loadReadings(this@CustomerViewModel, db)
            }
        }
    }

    fun loadHistory(db: AppDatabase) {
        viewModelScope.launch {
            history = db.historyDao().getHistory(customers[currentIndex].srvc_nmbr.substring(0, 8))
            val last3 = history.takeLast(3)
            if (last3.isNotEmpty()) {
                averageLast3 =  last3.map{it.consume}.average().toInt()
            } else {
                averageLast3 = 0
            }
        }
    }

    fun loadFindings(db: AppDatabase) {
        viewModelScope.launch {
            findings = db.findingsDao().getFldFindings()
        }
    }

    fun loadWaterRates(db: AppDatabase) {
        viewModelScope.launch {
            waterRates = db.waterRatesDao().getAllWaterRates()
        }
    }

    fun getRates(acctCode: String, db: AppDatabase) {
        viewModelScope.launch {
            acctRates = db.waterRatesDao().getWaterRates(acctCode)
        }
    }

    fun next() {
        if (currentIndex < customers.size - 1) currentIndex++
    }

    fun previous() {
        if (currentIndex > 0) currentIndex--
    }
}

data class ImportResult(
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList(),
    val isImporting: Boolean = false,
    val isDone: Boolean = false
)

class CustViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    val customers: Flow<List<CustomerInfo>> = db.customerDao().getAllCustomers()
    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun downloadCustomers(context: Context, readDate: String) {
        // Using UserSession values with fallback to the ones from your error message
        val ip = UserSession.ipAddr ?: "10.0.0.203"
        val port = UserSession.svrPort ?: "8080"
        // Removed ?wsdl from the endpoint
        val url = "http://$ip:$port/RnBWebService/services/ReadBill"

        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()
            try {
                val request = SoapObject("http://rnbWS", "downloadInfo")
                request.addProperty("user", UserSession.readerId ?: "")
                request.addProperty("read_date", readDate ?: "")
                val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
                envelope.dotNet = false
                envelope.setOutputSoapObject(request)

                val transport = HttpTransportSE(url)
                // Action provided: http://rnbWS/getFindings
                transport.call("http://rnbWS/downloadInfo", envelope)

                val response = envelope.response?.toString() ?: ""
                if (response.isNotBlank()) {
                    val records = response.split("|")
                    val batch = mutableListOf<CustomerInfo>()
                    db.customerDao().deleteAll()
                    records.forEach { record ->
                        if (record.isNotBlank()) {
                            val parts = record.split("$")
                            if (parts.size >= 17) {
                                batch.add(CustomerInfo(
                                    srvc_nmbr = parts[2], read_seqn = parts[0].toInt(), acct_nmbr = parts[1],
                                    cssr_name = parts[3], cssr_addr = parts[4], mtr_info = parts[5],
                                    prev_rdng = parts[6].toString().trim().toIntOrNull() ?: 0, date_from = parts[7],
                                    amt_arr = parts[8].toDoubleOrNull() ?: 0.0, prev_arr = parts[9].toDoubleOrNull() ?: 0.0,
                                    amt_misc = parts[10].toDoubleOrNull() ?: 0.0,
                                    amt_mat = parts[11].toDoubleOrNull() ?: 0.0, amt_pdv = parts[12].toDoubleOrNull() ?: 0.0,
                                    amt_aro = parts[13].toDoubleOrNull() ?: 0.0, months = parts[14].toInt(),
                                    average = parts[17].toInt(),
                                    stat_code = parts[15], location = parts[18], s_citizen = parts[20],
                                    s_expire = " ", chk_sum = 0.00
                                ))
                                successCount++
                            } else {
                                UserSession.readDate = parts[1]
                                UserSession.dueDate = parts[2]
                                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putString("read_date", parts[1])
                                    putString("due_date", parts[2])
                                    apply() // apply() is asynchronous and safe for background threads
                                }
                            }
                        }
                        if (batch.size == 100) {
                            db.customerDao().insertCustomers(batch)
                            batch.clear()
                        }
                    }
                    if (batch.isNotEmpty()) {
                        db.customerDao().insertCustomers(batch)
                    }
                }
            } catch (e: Exception) {
                errorCount++
                errors.add("Download error: ${e.localizedMessage}")
            } finally {
                importResult = ImportResult(successCount, errorCount, errors, false, true)
            }
        }
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)

            // Delete all existing customer info before starting the import
            db.customerDao().deleteAll()
            parseAndInsert(context, uri)
        }
    }

    private suspend fun parseAndInsert(context: Context, uri: Uri) {
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val batch = mutableListOf<CustomerInfo>()
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = parseLine(line)
                    if (parts.size >= 24) {
                        try {
                            batch.add(CustomerInfo(
                                srvc_nmbr = parts[6], read_seqn = parts[12].toInt(), acct_nmbr = parts[5],
                                cssr_name = parts[0], cssr_addr = parts[1], mtr_info = parts[2],
                                prev_rdng = parts[8].toString().trim().toIntOrNull() ?: 0, date_from = parts[19],
                                amt_arr = parts[13].toDouble(), prev_arr = 0.00, amt_misc = parts[14].toDoubleOrNull() ?: 0.0,
                                amt_mat = parts[15].toDoubleOrNull() ?: 0.0, amt_pdv = parts[16].toDoubleOrNull() ?: 0.0,
                                amt_aro = parts[17].toDoubleOrNull() ?: 0.0, months = parts[18].toInt(),
                                average = ((parts[9].toString().trim().toIntOrNull() ?: 0) +
                                        (parts[10].toString().trim().toIntOrNull() ?: 0) +
                                        (parts[11].toString().trim().toIntOrNull() ?: 0)) / 3,
                                stat_code = parts[7], location = parts[22], s_citizen = parts[20],
                                s_expire = parts[21], chk_sum = 0.00
                            ))
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    }
                    if (batch.size == 100) {
                        db.customerDao().insertCustomers(batch)
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) db.customerDao().insertCustomers(batch)
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(successCount, errorCount, errors, false, true)
        }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                ';' -> {
                    if (inQuotes) current.append(char)
                    else { result.add(current.toString()); current = StringBuilder() }
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}

class HistoryViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)
            db.historyDao().deleteAllHistories()
            parseAndInsert(context, uri)
        }
    }

    private suspend fun parseAndInsert(context: Context, uri: Uri) {
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val batch = mutableListOf<ReadHistory>()
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                // skip the first line
                lines.drop(1).forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = parseLine1(line)
                    if (parts.size >= 6) {
                        try {
                            batch.add(ReadHistory(
                                srvc_nmbr = parts[0], read_date = parts[1], pres_rdng = parts[2].toInt(),
                                consume = parts[3].toInt(), remarks = parts[4], reader = parts[5]
                            ))
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    }
                    if (batch.size == 100) {
                        db.historyDao().insertHistories(batch)
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) db.historyDao().insertHistories(batch)
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(successCount, errorCount, errors, false, true)
        }
    }
}

class CustViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CustViewModel(db) as T
}

class HistoryViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(db) as T
}

fun parseLine1(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    for (char in line) {
        when (char) {
            '"' -> inQuotes = !inQuotes
            ',' -> {
                if (inQuotes) current.append(char)
                else { result.add(current.toString()); current = StringBuilder() }
            }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}
