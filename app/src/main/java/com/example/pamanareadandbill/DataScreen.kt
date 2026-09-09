package com.example.pamanareadandbill

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataScreen(navController: NavController) {
    Column {
        DataTab(navController)
    }
}

@Composable
fun DataTab(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Download", "Upload", "Update")

    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)

    val viewModel: CustViewModel = viewModel(
        factory = CustViewModelFactory(db)
    )

    Column {
        TitleBar(stringResource(com.example.pamanareadandbill.R.string.tb_data))
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(text= title, fontSize = 14.sp) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    selectedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unselectedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
        when (selectedTabIndex) {
            0 -> DownloadTabContent(viewModel)
            1 -> UploadTabContent()
            2 -> UpdateTabContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTabContent(viewModel: CustViewModel) {

    val context = LocalContext.current
    val db2 = DatabaseProvider.getDatabase(context)
    val viewModel2: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(db2)
    )

    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf("No date selected") }

    val datePickerState = rememberDatePickerState()

    val importResult = viewModel.importResult
    val importResult2 = viewModel2.importResult

    //val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCsv(context, it)
        }
    }

    val launcher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel2.importCsv(context, it)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = selectedDate,
                readOnly = true,
                onValueChange = {
                    selectedDate = it
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Read Date", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !importResult.isImporting
                ) {
                    Text("Select Date")
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = !importResult.isImporting
                ) {
                    Text("Download")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        launcher.launch("text/*")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !importResult.isImporting
                ) {
                    Text("Load from File")
                }
                Button(
                    onClick = {
                         launcher2.launch("text/*")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !importResult.isImporting
                ) {
                    Text("Load Histories")
                }
            }

            if (importResult.isImporting || importResult2.isImporting) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator()
                Text("Importing data...", modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (showDialog) {
            DatePickerDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            selectedDate = formatter.format(Date(millis))
                        }
                        showDialog = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // --- IMPORT RESULTS FOR CUSTOMERS ----------------------------------------
        AlertDoneImporting(importResult, viewModel)
        /*
        if (importResult.isDone) {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportResult() },
                title = { Text(if (importResult.errorCount == 0) "Import Successful" else "Import Completed with Errors") },
                text = {
                    Column {
                        Text("Success: ${importResult.successCount}")
                        Text("Errors: ${importResult.errorCount}")
                        if (importResult.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Error details:", fontWeight = FontWeight.Bold)
                            // Only show first 5 errors to avoid huge dialogs
                            importResult.errors.take(5).forEach { error ->
                                Text("• $error", fontSize = 12.sp)
                            }
                            if (importResult.errors.size > 5) {
                                Text("... and ${importResult.errors.size - 5} more", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.resetImportResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        */
        // --------------------------------------------------------------------

        // ---- IMPORT RESULTS FOR HISTORIES ---------------------------------
        AlertDoneImporting(importResult2, viewModel2)
        /*
        if (importResult2.isDone) {
            AlertDialog(
                onDismissRequest = { viewModel2.resetImportResult() },
                title = { Text(if (importResult2.errorCount == 0) "Import Successful" else "Import Completed with Errors") },
                text = {
                    Column {
                        Text("Success: ${importResult2.successCount}")
                        Text("Errors: ${importResult2.errorCount}")
                        if (importResult2.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Error details:", fontWeight = FontWeight.Bold)
                            // Only show first 5 errors to avoid huge dialogs
                            importResult2.errors.take(5).forEach { error ->
                                Text("• $error", fontSize = 12.sp)
                            }
                            if (importResult2.errors.size > 5) {
                                Text("... and ${importResult2.errors.size - 5} more", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel2.resetImportResult() }) {
                        Text("OK")
                    }
                }
            )
        }*/

        //--------------------------------------------------------------
    }

}

@Preview(showBackground = true)
@Composable
fun DownloadTabContentPreview() {
    /*val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val viewModel: CustViewModel = viewModel(
        factory = CustViewModelFactory(db)
    )
    DownloadTabContent(viewModel)*/
}

@Composable
fun UploadTabContent() {
    Text("Upload Tab Content")
}

@Composable
fun UpdateTabContent() {

    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val viewModel1: FindingsViewModel = viewModel(
        factory = FindingsViewModelFactory(db)
    )

    val viewModel2: RatesViewModel = viewModel(
        factory = RatesViewModelFactory(db)
    )

    val viewModel3: ReadersViewModel = viewModel(
        factory = ReadersViewModelFactory(db)
    )

    val importResult = viewModel1.importResult
    val importResult2 = viewModel2.importResult
    val importResult3 = viewModel3.importResult

    val launcher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel1.importCsv(context, it)
        }
    }

    val launcher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel2.importCsv(context, it)
        }
    }

    val launcher3 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {uri: Uri? ->
        uri?.let {
            viewModel3.importCsv(context,it)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    //launcher3.launch("text/*")
                    viewModel3.downloadReaders()
                },
                enabled = !importResult3.isImporting,
                modifier = Modifier
                    .width(300.dp)
            ) {
                Text("Update Meter Readers")
            }
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    viewModel1.downloadFindings()
                    //launcher1.launch("text/*")
                },
                enabled = !importResult.isImporting,
                modifier = Modifier
                    .width(300.dp)
            ) {
                Text("Update Field Findings")
            }
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    launcher2.launch("text/*")
                },
                enabled = !importResult2.isImporting,
                modifier = Modifier
                    .width(300.dp)
            ) {
                Text("Update Water Rates")
            }
            if (importResult.isImporting || importResult2.isImporting || importResult3.isImporting) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator()
                Text("Importing data...", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    // Done importing field findings...
    AlertDoneImporting(importResult, viewModel1)
    /*
    if (importResult.isDone) {
        AlertDialog(
            onDismissRequest = { viewModel1.resetImportResult() },
            title = { Text(if (importResult.errorCount == 0) "Import Successful" else "Import Completed with Errors") },
            text = {
                Column {
                    Text("Success: ${importResult.successCount}")
                    Text("Errors: ${importResult.errorCount}")
                    if (importResult.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error details:", fontWeight = FontWeight.Bold)
                        // Only show first 5 errors to avoid huge dialogs
                        importResult.errors.take(5).forEach { error ->
                            Text("• $error", fontSize = 12.sp)
                        }
                        if (importResult.errors.size > 5) {
                            Text("... and ${importResult.errors.size - 5} more", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel1.resetImportResult() }) {
                    Text("OK")
                }
            }
        )
    }*/

    // Done importing water rates...
    AlertDoneImporting(importResult2, viewModel2)
    // Done importing meter readers...
    AlertDoneImporting(importResult3, viewModel3)
    /*
    if (importResult2.isDone) {
        AlertDialog(
            onDismissRequest = { viewModel2.resetImportResult() },
            title = { Text(if (importResult2.errorCount == 0) "Import Successful" else "Import Completed with Errors") },
            text = {
                Column {
                    Text("Success: ${importResult2.successCount}")
                    Text("Errors: ${importResult2.errorCount}")
                    if (importResult2.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error details:", fontWeight = FontWeight.Bold)
                        // Only show first 5 errors to avoid huge dialogs
                        importResult2.errors.take(5).forEach { error ->
                            Text("• $error", fontSize = 12.sp)
                        }
                        if (importResult2.errors.size > 5) {
                            Text("... and ${importResult2.errors.size - 5} more", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel2.resetImportResult() }) {
                    Text("OK")
                }
            }
        )
    }*/
}

@Composable
fun AlertDoneImporting(importResult: ImportResult, viewModel: ResettableImport) {

    if (importResult.isDone) {
        AlertDialog(
            onDismissRequest = { viewModel.resetImportResult() },
            title = { Text(if (importResult.errorCount == 0) "Import Successful" else "Import Completed with Errors") },
            text = {
                Column {
                    Text("Success: ${importResult.successCount}")
                    Text("Errors: ${importResult.errorCount}")
                    if (importResult.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error details:", fontWeight = FontWeight.Bold)
                        // Only show first 5 errors to avoid huge dialogs
                        importResult.errors.take(5).forEach { error ->
                            Text("• $error", fontSize = 12.sp)
                        }
                        if (importResult.errors.size > 5) {
                            Text("... and ${importResult.errors.size - 5} more", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.resetImportResult() }) {
                    Text("OK")
                }
            }
        )
    }

}

@Preview(showBackground = true)
@Composable
fun UpdateTabContentPreview() {
    UpdateTabContent()
}