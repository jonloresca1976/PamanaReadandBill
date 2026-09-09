package com.example.pamanareadandbill

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.io.BufferedReader
import java.io.InputStreamReader

// ResettableImport is already defined in ViewModels.kt, do not redefine here.

class FindingsViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun downloadFindings() {
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
                val request = SoapObject("http://rnbWS", "getFindings")
                val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
                envelope.dotNet = false
                envelope.setOutputSoapObject(request)

                val transport = HttpTransportSE(url)
                // Action provided: http://rnbWS/getFindings
                transport.call("http://rnbWS/getFindings", envelope)

                val response = envelope.response?.toString() ?: ""
                if (response.isNotBlank()) {
                    val records = response.split("#")
                    val batch = mutableListOf<FieldFindings>()
                    records.forEach { record ->
                        if (record.isNotBlank()) {
                            val fields = record.split("$")
                            if (fields.size >= 2) {
                                batch.add(FieldFindings(finding_desc = fields[0], endorsed_to = fields[1]))
                                successCount++
                            }
                        }
                        if (batch.size == 100) {
                            db.findingsDao().insertFldFindings(batch)
                            batch.clear()
                        }
                    }
                    if (batch.isNotEmpty()) {
                        db.findingsDao().deleteAllFldFindings()
                        db.findingsDao().insertFldFindings(batch)
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
            db.findingsDao().deleteAllFldFindings()
            parseAndInsert(context, uri)
        }
    }

    private suspend fun parseAndInsert(context: Context, uri: Uri) {
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val batch = mutableListOf<FieldFindings>()
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                // skip the first line
                lines.drop(1).forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = parseLine1(line)
                    if (parts.size >= 2) {
                        try {
                            batch.add(FieldFindings(finding_desc = parts[0], endorsed_to = parts[1]))
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    }
                    if (batch.size == 100) {
                        db.findingsDao().insertFldFindings(batch)
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) db.findingsDao().insertFldFindings(batch)
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(successCount, errorCount, errors, false, true)
        }
    }
}

class FindingsViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FindingsViewModel(db) as T
}

class RatesViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)

            // Delete all existing Water Rates before starting the import
            db.waterRatesDao().deleteAllWaterRates()
            parseAndInsert(context, uri)
        }
    }

    private suspend fun parseAndInsert(context: Context, uri: Uri) {
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val batch = mutableListOf<WaterRates>()
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = parseLine1(line)
                    if (parts.size >= 17) {
                        try {
                            batch.add(WaterRates(
                                acct_code = parts[0], acct_desc = parts[1],
                                low_lim1 = parts[2].toInt(), high_lim1 = parts[3].toInt(), amt1 = parts[4].toDouble(),
                                low_lim2 = parts[5].toInt(), high_lim2 = parts[6].toInt(), amt2 = parts[7].toDouble(),
                                low_lim3 = parts[8].toInt(), high_lim3 = parts[9].toInt(), amt3 = parts[10].toDouble(),
                                low_lim4 = parts[11].toInt(), high_lim4 = parts[12].toInt(), amt4 = parts[13].toDouble(),
                                low_lim5 = parts[14].toInt(), high_lim5 = parts[15].toInt(), amt5 = parts[16].toDouble()
                            ))
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    }
                    if (batch.size == 100) {
                        db.waterRatesDao().insertWaterRates(batch)
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) db.waterRatesDao().insertWaterRates(batch)
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(successCount, errorCount, errors, false, true)
        }
    }
}

class RatesViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = RatesViewModel(db) as T
}

class ReadersViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }


    fun downloadReaders() {
        // Using UserSession values with fallback to the ones from your error message
        val ip = UserSession.ipAddr ?: "10.0.0.203"
        val port = UserSession.svrPort ?: "80"
        // Removed ?wsdl from the endpoint
        val url = "http://$ip:$port/RnBWebService/services/ReadBill"

        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()
            try {
                // Changed to getUsers (plural) to match the SOAP_ACTION
                val request = SoapObject("http://rnbWS", "getUsers")
                val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
                envelope.dotNet = false
                envelope.setOutputSoapObject(request)
                
                val transport = HttpTransportSE(url)
                // Action provided: http://rnbWS/getUsers
                transport.call("http://rnbWS/getUsers", envelope)

                val response = envelope.response?.toString() ?: ""
                if (response.isNotBlank()) {
                    val records = response.split("#")
                    val batch = mutableListOf<MeterReaders>()
                    records.forEach { record ->
                        if (record.isNotBlank()) {
                            val fields = record.split("$")
                            if (fields.size >= 2) {
                                batch.add(MeterReaders(reader_id = fields[0], reader_name = fields[1], reader_pw = "1234", device_id = ""))
                                successCount++
                            }
                        }
                    }
                    if (batch.isNotEmpty()) {
                        db.meterReaderDao().deleteAllMeterReaders()
                        db.meterReaderDao().insertMeterReaders(batch)
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
            db.meterReaderDao().deleteAllMeterReaders()
            parseAndInsert(context, uri)
        }
    }

    private suspend fun parseAndInsert(context: Context, uri: Uri) {
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val batch = mutableListOf<MeterReaders>()
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                // skip the first line
                lines.drop(1).forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = parseLine1(line)
                    if (parts.size >= 4) {
                        try {
                            batch.add(MeterReaders(reader_id = parts[0], reader_name = parts[1], reader_pw = parts[2], device_id = parts[3]))
                            successCount++
                        } catch (e: Exception) {
                            errorCount++
                            errors.add("Line ${index + 1}: ${e.localizedMessage}")
                        }
                    }
                    if (batch.size == 100) {
                        db.meterReaderDao().insertMeterReaders(batch)
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) db.meterReaderDao().insertMeterReaders(batch)
        } catch (e: Exception) {
            errorCount++
            errors.add("File error: ${e.localizedMessage}")
        } finally {
            importResult = ImportResult(successCount, errorCount, errors, false, true)
        }
    }
}

class ReadersViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ReadersViewModel(db) as T
}
