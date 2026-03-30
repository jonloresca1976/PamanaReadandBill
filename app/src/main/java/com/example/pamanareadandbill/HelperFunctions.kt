package com.example.pamanareadandbill

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*

fun populateData(context: Context) {
    val db = DatabaseProvider.getDatabase(context)

    // Note: Room operations should ideally be run on a background thread.
    // For now, we fix the compiler error by passing the context.
    db.customerDao().insertCustomer(
        CustomerInfo("00000003",
            1, "345678912", "Johanna Patrice", "Bayambang", "1234567890",
            50, "2023-01-01", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12,
            average = 1, stat_code = "I", location = "Bayambang", s_citizen = "No",
            s_expire = "2024-12-31", chk_sum = 123456789.0
        )
    )
}

fun clearFields(viewModel: CustomerViewModel) {
    viewModel.updateReading("")
    viewModel.updateConsumption("")
    viewModel.updateValue("")
    viewModel.updateSelectedFinding("                ")
    viewModel.updateRemarks("")
}

fun computeValue(cons: Int, acctNmbr: String, viewModel: CustomerViewModel) {

    val acctCode = acctNmbr.drop(3).take(3)
    var value: Double = 0.00
    val rate = viewModel.waterRates.find { it.acct_code == acctCode }

    if (rate != null) {
        if (cons >= rate.low_lim1 && cons <= rate.high_lim1) {
            value = rate.amt1
        }
        if(cons >= rate.low_lim2 && cons <= rate.high_lim2) {
            value = rate.amt1 +
                    ((cons - rate.high_lim1) * rate.amt2)
        }
        if(cons >= rate.low_lim3 && cons <= rate.high_lim3) {
            value = rate.amt1 +
                    (10 * rate.amt2) +
                    ((cons - rate.high_lim2) * rate.amt3)
        }
        if(cons >= rate.low_lim4 && cons <= rate.high_lim4) {
            value = rate.amt1 +
                    (10 * rate.amt2) +
                    (10 * rate.amt3) +
                    ((cons - rate.high_lim3) * rate.amt4)
        }
        if(cons >= rate.low_lim5) {
            value = rate.amt1 +
                    (10 * rate.amt2) +
                    (10 * rate.amt3) +
                    (10 * rate.amt4) +
                    ((cons - rate.high_lim4) * rate.amt5)
        }
    }
    viewModel.updateValue(value.toString())
}

suspend fun saveReading(viewModel: CustomerViewModel, db: AppDatabase) {
    withContext(Dispatchers.IO) {
        val currentCustomer = viewModel.customers[viewModel.currentIndex]
        var readingData = db.meterReadingDao().getMeterReading(currentCustomer.srvc_nmbr)
        
        if (readingData == null) {
            readingData = MeterReading(
                srvc_nmbr = currentCustomer.srvc_nmbr,
                read_date = "2026-03-01", // TODO: Add real date later
                prev_rdng = currentCustomer.prev_rdng,
                pres_rdng = viewModel.reading.toIntOrNull() ?: 0,
                consume = viewModel.consumption.toIntOrNull() ?: 0,
                peso_value = viewModel.pesoValue.toDoubleOrNull() ?: 0.0,
                amt_arr = currentCustomer.amt_arr,
                prev_arr = 0.00,
                amt_others = currentCustomer.amt_misc +
                        currentCustomer.amt_mat +
                        currentCustomer.amt_pdv +
                        currentCustomer.amt_aro,
                field_findings = viewModel.selectedFinding,
                remarks = viewModel.remarks,
                reader = "JONATHAN",
                numb_tries = 0,
                numb_print = 0,
                read_time = "2026-03-01",
                read_loc = " ",
                loc_update = " ",
                device_id = "ABC123 "
            )
            db.meterReadingDao().insertMeterReading(readingData!!)
        } else {
            // Already exists - maybe update? For now just keeping it as is to match previous behavior
            // where it just shows the dialog.
        }
    }
}

suspend fun loadReadings(viewModel: CustomerViewModel, db: AppDatabase) {
    withContext(Dispatchers.IO) {
        val currentCustomer = viewModel.customers[viewModel.currentIndex]
        var readingData = db.meterReadingDao().getMeterReading(currentCustomer.srvc_nmbr)

        if (readingData != null) {
            viewModel.updateReading(readingData?.pres_rdng.toString())
            viewModel.updateConsumption(readingData?.consume.toString())
            viewModel.updateValue(readingData?.peso_value.toString())
            viewModel.updateSelectedFinding(readingData?.field_findings.toString())
            viewModel.updateRemarks(readingData?.remarks.toString())
        }

    }
}
