package com.example.pamanareadandbill

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

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