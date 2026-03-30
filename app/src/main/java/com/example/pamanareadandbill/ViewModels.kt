package com.example.pamanareadandbill

import android.content.Context
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.String

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

    // Add a function to update the reading
    fun updateReading(value: String) { reading = value }
    fun updateConsumption(value: String) { consumption = value }
    fun updateValue(value: String) { pesoValue = value }
    fun updateSelectedFinding(value: String) { selectedFinding = value }
    fun updateRemarks(value: String) { remarks = value }

    fun loadCustomers(db: AppDatabase) {
        viewModelScope.launch {
            customers = db.customerDao().getCustomers()
        }
    }

    fun loadHistory(db: AppDatabase) {
        viewModelScope.launch {
            history = db.historyDao().getHistory(customers[currentIndex].srvc_nmbr.substring(0, 8))
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
                            batch.add(
                                CustomerInfo(
                                    srvc_nmbr = parts[6],
                                    read_seqn = parts[12].toInt(),
                                    acct_nmbr = parts[5],
                                    cssr_name = parts[0],
                                    cssr_addr = parts[1],
                                    mtr_info = parts[2],
                                    prev_rdng = parts[8].toString().trim().toIntOrNull() ?: 0,
                                    date_from = parts[19],
                                    amt_arr = parts[13].toDouble(),
                                    prev_arr = 0.00,
                                    amt_misc = parts[14].toDoubleOrNull() ?: 0.0,
                                    amt_mat = parts[15].toDoubleOrNull() ?: 0.0,
                                    amt_pdv = parts[16].toDoubleOrNull() ?: 0.0,
                                    amt_aro = parts[17].toDoubleOrNull() ?: 0.0,
                                    months = parts[18].toInt(),
                                    average = ((parts[9].toString().trim().toIntOrNull() ?: 0) +
                                            (parts[10].toString().trim().toIntOrNull() ?: 0) +
                                            (parts[11].toString().trim().toIntOrNull() ?: 0)) / 3,
                                    stat_code = parts[7],
                                    location = parts[22],
                                    s_citizen = parts[20],
                                    s_expire = parts[21],
                                    chk_sum = 0.00
                                )
                            )
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    } else {
                        errorCount++
                        errors.add("Line ${index + 1}: Invalid column count (${parts.size})")
                    }

                    if (batch.size == 100) {
                        db.customerDao().insertCustomers(batch)
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                db.customerDao().insertCustomers(batch)
            }
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(
                successCount = successCount,
                errorCount = errorCount,
                errors = errors,
                isImporting = false,
                isDone = true
            )
        }
    }

    // Handles ";" and quoted values
    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                ';' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }

        result.add(current.toString())
        return result
    }
}


 class HistoryViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
     val histories: Flow<List<CustomerInfo>> = db.customerDao().getAllCustomers()

    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)

            // Delete all existing histories before starting the import
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
                            batch.add(
                                ReadHistory(
                                    srvc_nmbr = parts[0],
                                    read_date = parts[1],
                                    pres_rdng = parts[2].toInt(),
                                    consume   = parts[3].toInt(),
                                    remarks   = parts[4],
                                    reader    = parts[5]
                                )
                            )
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    } else {
                        errorCount++
                        errors.add("Line ${index + 1}: Invalid column count (${parts.size})")
                    }

                    if (batch.size == 100) {
                        db.historyDao().insertHistories(batch)
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                db.historyDao().insertHistories(batch)
            }
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(
                successCount = successCount,
                errorCount = errorCount,
                errors = errors,
                isImporting = false,
                isDone = true
            )
        }
    }
}

class CustViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustViewModel(db) as T
    }
}

class HistoryViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(db) as T
    }
}

fun parseLine1(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when (char) {
            '"' -> inQuotes = !inQuotes
            ',' -> {
                if (inQuotes) {
                    current.append(char)
                } else {
                    result.add(current.toString())
                    current = StringBuilder()
                }
            }
            else -> current.append(char)
        }
    }

    result.add(current.toString())
    return result
}
