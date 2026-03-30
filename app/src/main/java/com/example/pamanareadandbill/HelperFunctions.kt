package com.example.pamanareadandbill

import android.content.Context
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
