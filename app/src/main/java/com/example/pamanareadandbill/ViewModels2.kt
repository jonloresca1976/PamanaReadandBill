package com.example.pamanareadandbill

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
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

interface ResettableImport {
    fun resetImportResult()
}

class FindingsViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    // val findings: Flow<List<FieldFindings>> = db.findingsDao().getFldFindings()

    var importResult by mutableStateOf(ImportResult())
        private set

    override fun resetImportResult() {
        importResult = ImportResult()
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult = importResult.copy(isImporting = true, isDone = false)

            // Delete all existing histories before starting the import
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
                            batch.add(
                                FieldFindings(
                                    finding_desc = parts[0],
                                    endorsed_to  = parts[1]
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
                        db.findingsDao().insertFldFindings(batch)
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                db.findingsDao().insertFldFindings(batch)
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

class FindingsViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FindingsViewModel(db) as T
    }
}

class RatesViewModel(private val db: AppDatabase) : ViewModel(), ResettableImport {
    // val findings: Flow<List<FieldFindings>> = db.findingsDao().getFldFindings()

    var importResult by mutableStateOf(ImportResult())
        private set

    override  fun resetImportResult() {
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
                // skip the first line
                lines.drop(1).forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed

                    val parts = parseLine1(line)

                    if (parts.size >= 17) {
                        try {
                            batch.add(
                                WaterRates(
                                    acct_code = parts[0],
                                    acct_desc = parts[1],
                                    low_lim1  = parts[2].toInt(),
                                    high_lim1 = parts[3].toInt(),
                                    amt1      = parts[4].toDouble(),
                                    low_lim2  = parts[5].toInt(),
                                    high_lim2 = parts[6].toInt(),
                                    amt2      = parts[7].toDouble(),
                                    low_lim3  = parts[8].toInt(),
                                    high_lim3 = parts[9].toInt(),
                                    amt3      = parts[10].toDouble(),
                                    low_lim4  = parts[11].toInt(),
                                    high_lim4 = parts[12].toInt(),
                                    amt4      = parts[13].toDouble(),
                                    low_lim5  = parts[14].toInt(),
                                    high_lim5 = parts[15].toInt(),
                                    amt5      = parts[16].toDouble()
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
                        db.waterRatesDao().insertWaterRates(batch)
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                db.waterRatesDao().insertWaterRates(batch)
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

class RatesViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RatesViewModel(db) as T
    }
}