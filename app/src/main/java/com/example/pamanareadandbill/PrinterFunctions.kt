package com.example.pamanareadandbill

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.IOException

fun samplePrint(context: Context) {
    val sb = StringBuilder()

    sb.append("<111>PAMANA WATER CORPORATION")
    sb.append("<110>Dagupan City")
    sb.append("<110>Tel. Nos.:653-2229, 0917-8428653\n")
    sb.append("<110>STATEMENT OF ACCOUNT")
    sb.append("<110>FOR THE MONTH OF: MARCH 2026\n")
    sb.append("<101>LORESCA, JONATHAN")
    sb.append("<100>BAYAMBANG, PANGASINAN\n")

    val str = sb.toString()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        // Specific package for the "Bluetooth Print" app
        setPackage("mate.bluetoothprint")
        putExtra(Intent.EXTRA_TEXT, str)
        type = "text/plain"

        // Essential if calling from a ViewModel or background context
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(sendIntent)
    } catch (e: ActivityNotFoundException) {
        // Handle the case where the external app is not installed
        Toast.makeText(context, "Bluetooth Print app is not installed.", Toast.LENGTH_LONG).show()

        // Optional: Redirect to Play Store
        // val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=mate.bluetoothprint"))
        // context.startActivity(playStoreIntent)
    }
}

fun printBill(context: Context, viewModel: CustomerViewModel) {
    val sb = StringBuilder()

    val sdf = java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", java.util.Locale.getDefault())
    val currentDateTime = sdf.format(java.util.Date())

    val customer = viewModel.customers[viewModel.currentIndex]
    val rawAcct = customer.acct_nmbr
    val formattedAcct = if (rawAcct.length >= 13) {
        "${rawAcct.substring(0, 3)}-${rawAcct.substring(3, 6)}-${rawAcct.substring(6,10)}-${rawAcct.substring(10, 12)}-${rawAcct.substring(12)}"
    } else {
        rawAcct
    }
    val scNumber = customer.srvc_nmbr.substring(0, 8) + "-" + customer.srvc_nmbr.substring(8)
    val referenceNo = "007" + customer.srvc_nmbr.substring(0, 8)

    var subTotal = 0.00
    var vat = viewModel.pesoValue.toDouble() * 0.12
    subTotal = viewModel.pesoValue.toDouble() + vat
    var penalty = subTotal * 0.10

    //-----------------------------------------------------------------------
    // Compute for Senior Citizen discount
    //-----------------------------------------------------------------------
    var discount = 0.00
    var discAmount = 0.00
    if(customer.s_citizen == "T" || customer.s_citizen == "S") {
        if(viewModel.consumption.toInt() <= 30) {
            discount = (viewModel.pesoValue.toDouble() + vat) * 0.05
            discAmount = viewModel.pesoValue.toDouble() + vat - discount
            penalty = discAmount * 0.10
        }
    }
    //-----------------------------------------------------------------------

    var runningTotal = 0.00
    //----------------------------------------------------------------------------------------------------------------
    // How to deal with customers with negative balance
    //----------------------------------------------------------------------------------------------------------------
    if(customer.amt_arr + viewModel.pesoValue.toDouble() > 0) {
        if (customer.amt_arr < 0) {
            if ((customer.s_citizen == "T" || customer.s_citizen == "S") && (viewModel.consumption.toInt() <= 30)) {
                penalty = (discAmount - customer.amt_arr) * 0.10
            } else {
                penalty = (viewModel.pesoValue.toDouble() + vat + customer.amt_arr) * 0.10
            }
        }
    } else {
        penalty = 0.00
    }
    //----------------------------------------------------------------------------------------------------------------

    val base64 = try {
        val file =
            File(Environment.getExternalStorageDirectory(), "TEMP/TEST/logo.jpg")
        if (file.exists()) {
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else {
            Toast.makeText(context, "Logo file not found: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            null
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error reading logo: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
        null
    }
    
    sb.append("<IMAGE>1#"+base64)
    sb.append("<111>PAMANA WATER CORPORATION")
    sb.append("<110>Dagupan City")
    sb.append("<110>Tel. Nos.:653-2229, 0917-8428653\n")
    sb.append("<110>STATEMENT OF ACCOUNT")
    sb.append("<110>FOR THE MONTH OF: MARCH 2026\n")
    sb.append("<101>${customer.cssr_name.trimEnd()}")
    sb.append("<100>${customer.cssr_addr.trimEnd()}")
    sb.append("<000>Reference #: $referenceNo")
    sb.append("<000>Account   #: $formattedAcct")
    sb.append("<000>Srvc Conn #: $scNumber")
    sb.append("<000>" + "-".repeat(32))
    sb.append("<000>Meter. Nmbr: ${customer.mtr_info.trimEnd()}")
    sb.append("<000>Date From  : ${customer.date_from}")
    sb.append("<000>Date To    : " + UserSession.readDate)
    sb.append("<000>" + "-".repeat(32))
    sb.append("<000>" + formatReceiptLine("Present  Reading:", viewModel.reading))
    sb.append("<000>" + formatReceiptLine("Previous Reading:", customer.prev_rdng.toString()))
    sb.append("<000>" + formatReceiptLine("Consumption     :", viewModel.consumption))
    sb.append("<000>" + "-".repeat(32))
    sb.append("<100>" + formatReceiptLine("Basic Charge :", "%,.2f".format(viewModel.pesoValue.toDouble())))
    sb.append("<000>" + formatReceiptLine("Add VAT (12%):", "%,.2f".format(vat)))
    sb.append("<100>" + formatReceiptLine("Sub-Total    :", "%,.2f".format(subTotal)))
    runningTotal = runningTotal + subTotal
    if ((customer.s_citizen == "T" || customer.s_citizen == "S") && (viewModel.consumption.toInt() <= 30)) {
        sb.append("<000>" + formatReceiptLine("Senior Disc. :", "%,.2f".format(discount)))
        runningTotal = runningTotal - discount
    }
    if (customer.amt_arr != 0.0) {
        sb.append("<000>" + formatReceiptLine("Curr. Arrears:", "%,.2f".format(customer.amt_arr)))
        runningTotal = runningTotal + customer.amt_arr
    }
    if (customer.prev_arr > 0.00) {
        sb.append("<000>" + formatReceiptLine("Prev. Arrears:", "%,.2f".format(customer.prev_arr)))
        runningTotal = runningTotal + customer.prev_arr
    }
    if (customer.amt_mat > 0.00) {
        sb.append("<000>" + formatReceiptLine("Materials    :", "%,.2f".format(customer.amt_mat)))
        runningTotal = runningTotal + customer.amt_mat
    }
    if (customer.amt_misc > 0.00) {
        sb.append("<000>" + formatReceiptLine("Materials    :", "%,.2f".format(customer.amt_misc)))
        runningTotal = runningTotal + customer.amt_misc
    }
    if (customer.amt_pdv > 0.00) {
        sb.append("<000>" + formatReceiptLine("Materials    :", "%,.2f".format(customer.amt_pdv)))
        runningTotal = runningTotal + customer.amt_pdv
    }
    sb.append("<000>" + "-".repeat(32))
    sb.append("<101>" + formatReceiptLine("TOTAL        :", "%,.2f".format(runningTotal)))
    sb.append("<100>TOTAL AFTER")
    sb.append("<100>" + formatReceiptLine((UserSession.dueDate ?: "") + "   :", "%,.2f".format(runningTotal + penalty)))
    sb.append("<000>" + "=".repeat(32))
    sb.append("<000>" + formatReceiptLine("Meter Reader:", UserSession.readerName ?: ""))
    sb.append("<000>" + formatReceiptLine("Remarks:", viewModel.selectedFinding))
    sb.append("<010>$currentDateTime")
    sb.append("<010>" + "Device ID: " + (UserSession.deviceId ?: ""))

    val message1 = "PLEASE SETTLE YOUR BALANCE\n" +
                   "ON OR BEFORE 03/18/2026\n" +
                   "TO AVOID DISCONNECTION."
    val message2 = "Thank your for paying on time."
    val message3 = "Payments accepted at Perez, City Hall, and Tambac offices. " +
                   "As well as Queen Bank, 7-Eleven, GCash, and other ECPAY outlets."
    val message4 = "PAALALA: INGATAN PO ANG BILL NA ITO\n" +
                   "This is NOT VALID as and OFFICIAL RECEIPT.\n"
    // val message5 = "Thank you for paying on time."
    val message6 = "Disregard arrears if payment was made."
    val message7 = "Your water service is scheduled\n" +
                   "FOR DISCONNECTION\n" +
                   "since you have ${customer.months} months\n" +
                   "of unpaid arrears."
    val message8 = "SENIOR CITIZEN DISCOUNT\n" +
                   "EXPIRES SOON. PLS. RENEW."
    val message9 = "SENIOR CITIZEN DISCOUNT\n" +
                   "HAS EXPIRED. PLS. RENEW."
    if(customer.amt_arr > 0.00) {
        if(customer.months > 1) {
            sb.append("<000>" + "=".repeat(32))
            sb.append("<100>$message7")
        } else {
            sb.append("<000>" + "=".repeat(32))
            sb.append("<100>$message1")
        }
    }
    if(customer.s_citizen == "S") {
        sb.append("<000>" + "=".repeat(32))
        sb.append("<100>$message8")
    }
    if(customer.s_citizen == "R") {
        sb.append("<000>" + "=".repeat(32))
        sb.append("<100>$message9")
    }
    sb.append("<000>" + "=".repeat(32))
    sb.append("<100>$message3")
    sb.append("<000>" + "-".repeat(32))
    sb.append("<100>$message4")
    if(customer.amt_arr <=0.0) {
        sb.append("<100>$message2")
    } else {
        sb.append("<100>$message6")
    }
    sb.append("\n\n\n")

    val str = sb.toString()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        // Specific package for the "Bluetooth Print" app
        setPackage("mate.bluetoothprint")
        putExtra(Intent.EXTRA_TEXT, str)
        type = "text/plain"

        // Essential if calling from a ViewModel or background context
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(sendIntent)
    } catch (e: ActivityNotFoundException) {
        // Handle the case where the external app is not installed
        Toast.makeText(context, "Bluetooth Print app is not installed.", Toast.LENGTH_LONG).show()

        // Optional: Redirect to Play Store
        // val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=mate.bluetoothprint"))
        // context.startActivity(playStoreIntent)
    }
}

fun formatReceiptLine(label: String, value: String, lineLength: Int = 32): String {
    // <000> is the prefix used in your current code for normal text

    // Calculate how many spaces are needed to fill the line
    val spaceCount = lineLength - label.length - value.length

    // Ensure at least one space if the text is too long
    val spaces = if (spaceCount > 0) " ".repeat(spaceCount) else " "

    return "$label$spaces$value"
}
