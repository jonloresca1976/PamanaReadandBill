package com.example.pamanareadandbill

import android.R
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pamanareadandbill.ui.theme.PamanaReadandBillTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ReadEntryScreen(navController: NavController) {
    Column {
        ReadingTab(navController)
    }
}

@Composable
fun ReadingTab(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Reading", "History", "Preview", "Loc/Picture")

    val viewModel: CustomerViewModel = viewModel()

    Column {
        TitleBar(stringResource(com.example.pamanareadandbill.R.string.tb_meter_reading))
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
            0 -> ReadingTabContent(viewModel)
            1 -> HistoryTabContent(viewModel)
            2 -> PreviewTabContent(viewModel)
            3 -> LocationTabContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTabContent(viewModel: CustomerViewModel) {

    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var scNumber by rememberSaveable { mutableStateOf("") }
    var meterInfo by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var scDisc by rememberSaveable { mutableStateOf("") }
    var prevReading by rememberSaveable { mutableStateOf(0) }
    var average by rememberSaveable { mutableStateOf(0) }
    //var reading by rememberSaveable { mutableStateOf("") }        // moved to ViewModel
    //var consumption by rememberSaveable { mutableStateOf("") }    // moved to ViewModel
    var value by rememberSaveable { mutableStateOf("") }
    //var remarks by rememberSaveable { mutableStateOf("") }        // moved to ViewModel
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showOverwriteDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var returnedValue by rememberSaveable { mutableStateOf("") }
    var errorDesc by rememberSaveable { mutableStateOf("") }
    var itemToSearch by rememberSaveable { mutableStateOf("") }
    // var custIndex by rememberSaveable { mutableStateOf(0) }      // moved to ViewModel
    var averagePrefix by rememberSaveable { mutableStateOf("") }
    var pendingNavigation by rememberSaveable {mutableStateOf<(() -> Unit)?>(null)}  // allows the app to suspend navigation
                                                                                             // when changes are not yet saved

    /*val  fieldFindings = listOf(
        "                ",
        "High Consumption",
        "Low Consumption",
        "Defective Meter",
        "Zero Consumption"
    )*/

    //var selectedFinding by rememberSaveable { mutableStateOf(fieldFindings[0]) }

    // *** RESTORE THIS IF SOMETHING WENT WRONG val viewModel: CustomerViewModel = viewModel()

    //Use values from ViewModel
    val selectedFinding = viewModel.selectedFinding
    val reading = viewModel.reading
    val consumption = viewModel.consumption
    val pesoValue = viewModel.pesoValue
    val remarks = viewModel.remarks

    LaunchedEffect(Unit) {
        viewModel.loadCustomers(db)
        viewModel.loadFindings(db)
        viewModel.loadWaterRates(db)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.loadCustomers(db)
            viewModel.loadFindings(db)
        }
    }

    //--------------------------------------------------------
    // A function that triggers a warning if the user tries
    //   to navigate away from the current customer without
    //   saving the changes.
    //--------------------------------------------------------
    val navigateWithWarning = { action: () -> Unit ->
        if(viewModel.isModified) {
            pendingNavigation = action // Trigger the dialog
        } else {
            action() // Navigates immediately
        }
    }
    //---------------------------------------------------------

    val fieldFindings = viewModel.findings

    val customers = viewModel.customers
    val index = viewModel.currentIndex

    // var customers by remember { mutableStateOf<List<CustomerInfo>>(emptyList()) }

    /*LaunchedEffect(Unit) {
        // customers = db.customerDao().getCustomers()
        withContext(Dispatchers.IO) {
            val result = db.customerDao().getCustomers()
            withContext(Dispatchers.Main) {
                customers = result
            }
        }
    }*/

    if (customers.isNotEmpty()) {
        name = customers[index].cssr_name
        address = customers[index].cssr_addr
        accountNumber = customers[index].acct_nmbr.substring(0, 12) + "-" + customers[index].acct_nmbr.substring(12)
        scNumber = customers[index].srvc_nmbr.substring(0, 8) + "-" + customers[index].srvc_nmbr.substring(8)
        meterInfo = customers[index].mtr_info
        status = if (customers[index].stat_code == "1") "Active" else "Inactive"
        scDisc = when (customers[index].s_citizen) {
            "T" -> "Active"
            "R" -> "Expired"
            "S" -> "Expires soon"
            else -> "None"
        }
        prevReading = customers[index].prev_rdng
        average = customers[index].average
        viewModel.loadHistory(db)
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
            Text(
                text = "Name: $name",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Text(
                text = "Address: $address",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Text(
                text = "Account Number: $accountNumber",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Text(
                text = "SC. Number: $scNumber",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Text(
                text = "Meter Info: $meterInfo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                )
                Text(
                    text = "SC Disc.: $scDisc",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P/R: $prevReading    AVG: $average/${viewModel.averageLast3}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                )
                Text(
                    text = "${index + 1} of ${customers.size}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                )

            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reading,
                    onValueChange = {
                        viewModel.updateReading(it) // Update ViewModel
                        averagePrefix = ""
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = {
                        Text(text = "Reading", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val r = reading.toIntOrNull() ?: 0
                        var cons = 0
                        if (r > prevReading) {
                            cons = r - prevReading
                            viewModel.updateConsumption(cons.toString()) // Update ViewModel
                            computeValue(
                                cons,
                                customers[index].acct_nmbr,
                                viewModel
                            ) // Update ViewModel
                        } else {
                            errorDesc = "Present reading must be greater than previous reading"
                            showErrorDialog = true
                        }
                        when (getConsumptionTrend(cons, viewModel.averageLast3)) {
                            "increase" -> {
                                errorDesc = "Consumption has increased by 30%."
                                showErrorDialog = true
                            }
                            "decrease" -> {
                                errorDesc = "Consumption has decreased by 30%."
                                showErrorDialog = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text( text = "Compute", fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        val r = prevReading + customers[index].average
                        viewModel.updateReading(r.toString()) // Update ViewModel
                        viewModel.updateConsumption(customers[index].average.toString()) // Update ViewModel
                        computeValue(customers[index].average, customers[index].acct_nmbr, viewModel) // Update ViewModel
                        averagePrefix = "AVG - "
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Average", fontSize = 14.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = consumption,
                    onValueChange = {
                        //consumption = it
                        viewModel.updateConsumption(it) // Update ViewModel
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text(text = "Consume", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        computeValue(consumption.toIntOrNull() ?: 0, customers[index].acct_nmbr, viewModel)
                        averagePrefix = "AVGP - "
                        when (getConsumptionTrend(consumption.toIntOrNull() ?: 0, viewModel.averageLast3)) {
                            "increase" -> {
                                errorDesc = "Consumption has increased by 30%."
                                showErrorDialog = true
                            }
                            "decrease" -> {
                                errorDesc = "Consumption has decreased by 30%."
                                showErrorDialog = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Compute", fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = pesoValue,
                    onValueChange = {
                        //value = it
                        viewModel.updateValue(it) // Update ViewModel
                    },
                    label = { Text(text = "Value", fontSize = 14.sp) },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Search",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        showDialog = true
                        itemToSearch = "Name"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Name", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        showDialog = true
                        itemToSearch = "Meter"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Meter", fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        showDialog = true
                        itemToSearch = "Position"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Position", fontSize = 14.sp)
                }
            }

            //-------------------------------------------------------------------------
            // A dialog box used for searching a customer, by name, meter, or position
            //-------------------------------------------------------------------------
            if (showDialog) {
                InputDialog(
                    onDismiss = {
                        showDialog = false
                    },
                    onConfirm = { value ->
                        returnedValue = value   // RECEIVE VALUE
                        navigateWithWarning {
                            if (itemToSearch == "Position") {
                                val pos = returnedValue.toIntOrNull() ?: 0
                                if (pos >= 1 && pos <= customers.size) {
                                    viewModel.moveTo(returnedValue.toInt() - 1)
                                } else {
                                    Toast.makeText(context, "Invalid position", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            if (itemToSearch == "Name") {
                                val searchResult = viewModel.customers.indexOfFirst {
                                    it.cssr_name.contains(returnedValue, ignoreCase = true)
                                }
                                if (searchResult != -1) {
                                    viewModel.moveTo(searchResult)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Customer not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            if (itemToSearch == "Meter") {
                                val searchResult = viewModel.customers.indexOfFirst {
                                    it.mtr_info.contains(returnedValue, ignoreCase = true)
                                }
                                if (searchResult != -1) {
                                    viewModel.moveTo(searchResult)
                                } else {
                                    Toast.makeText(context, "Meter not found", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            showDialog = false
                            clearFields(viewModel)
                            averagePrefix = ""
                            scope.launch {
                                loadReadings(viewModel, db)
                            }
                        }
                    },
                    searchItem = itemToSearch
                )
            }
            //-------------------------------------------------------------------------

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                value = selectedFinding,
                onValueChange = {},
                readOnly = true,
                label = { Text("FieldFindings") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .paddingFromBaseline(25.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    fieldFindings.forEach { finding ->
                        DropdownMenuItem(
                            text = { Text(finding.finding_desc) },
                            onClick = {
                                viewModel.updateSelectedFinding(averagePrefix + finding.finding_desc) // Update ViewModel
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = {
                    //remarks = it
                    viewModel.updateRemarks(it) // Update ViewModel
                },
                label = { Text("Remarks") },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        navigateWithWarning {
                            viewModel.previous()
                            clearFields(viewModel)
                            averagePrefix = ""
                            scope.launch {
                                viewModel.loadHistory(db)
                                loadReadings(viewModel, db)
                            }
                        }
                    },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("<<")
                }
                Button(
                    onClick = {
                        scope.launch {
                            val exists = withContext(Dispatchers.IO) {
                                db.meterReadingDao().getMeterReading(customers[index].srvc_nmbr) != null
                            }
                            //----------------------------------------------------------------------------
                            // When using average consumption, check if the user selected a finding
                            // If he did not, show an error message, and don't proceed with saving
                            val isUsingAverage = averagePrefix.isNotEmpty()
                            // Check if finding is empty, blank, or matches your default "empty" string
                            val noFindingSelected = selectedFinding.trim().isEmpty() || selectedFinding == "                "
                            if (isUsingAverage && noFindingSelected) {
                                errorDesc = "Please select a finding"
                                showErrorDialog = true
                                return@launch // Exit the coroutine
                            }
                            //----------------------------------------------------------------------------
                            if (exists) {
                                showOverwriteDialog = true
                            } else {
                                saveReading(viewModel, db)
                                showSaveConfirmDialog = true
                            }

                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    //Text(text = "Save", fontSize = 12.sp)
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f)
                ) {
                    //Text(text = "Print", fontSize = 12.sp)
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null
                    )
                }
                Button(
                    onClick = {
                        navigateWithWarning {
                            viewModel.next()
                            clearFields(viewModel)
                            averagePrefix = ""
                            scope.launch {
                                viewModel.loadHistory(db)
                                loadReadings(viewModel, db)
                            }
                        }
                    },
                    enabled = index < customers.size - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(">>")
                }
            }

            if (showSaveConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveConfirmDialog = false },
                    title = { Text("Save Reading") },
                    text = { Text("Reading successfully saved.") },
                    confirmButton = {
                        TextButton(onClick = { showSaveConfirmDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            if (showOverwriteDialog) {
                AlertDialog(
                    onDismissRequest = { showOverwriteDialog = false },
                    title = { Text("Overwrite Reading") },
                    text = { Text("Reading already exists. Overwrite?") },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                saveReading(viewModel, db)
                                showSaveConfirmDialog = true
                                showOverwriteDialog = false
                            }
                        }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOverwriteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("Error Encountered") },
                    text = { Text(errorDesc) },
                    confirmButton = {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            if (pendingNavigation !=null) {
                AlertDialog(
                    onDismissRequest = { pendingNavigation = null },
                    title = { Text("Unsaved Changes") },
                    text = { Text("You have unsaved changes for this customer. Do you want to discard them and continue?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val action = pendingNavigation
                                pendingNavigation = null
                                viewModel.resetModified() // You'll need to add this to your ViewModel
                                action?.invoke() // Execute the navigation action (next, previous, or search)
                            }
                        ) {
                            Text("Discard")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingNavigation = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

        }
    }
}

@Composable
fun ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    TODO("Not yet implemented")
}

@Composable
fun HistoryTabContent(viewModel: CustomerViewModel) {

    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)

    val index = viewModel.currentIndex
    val customers = viewModel.customers

    if (customers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No customer data loaded.")
        }
        return
    }

    val customer = customers[index]

    LaunchedEffect(customer.srvc_nmbr) {
        viewModel.loadHistory(db)
    }

    val rdhistory = viewModel.history

    val lastThreeConsumptions = rdhistory
        .takeLast(3)
        .map { it.consume }

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
            Text(
                text = "Name: ${customer.cssr_name}",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
            Text(
                text = "Account Number: ${customer.acct_nmbr}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(2.dp,
            top = 90.dp,
            bottom = 20.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeaderRow()
            }
            items(rdhistory) { history ->
                HistoryCard(history)
            }
        }

    }
}

@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(8.dp)
    ) {
        Text("Date", modifier = Modifier.weight(25f))
        Text("Rdng", modifier = Modifier.weight(13f))
        Text("Cons", modifier = Modifier.weight(12f))
        Text("Reader", modifier = Modifier.weight(20f))
        Text("Remarks", modifier = Modifier.weight(30f))
    }
}

@Composable
fun HistoryCard(history: ReadHistory) {
    Card(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RectangleShape
    ) {
        Row(modifier = Modifier.padding(5.dp)) {
            Text("${history.read_date}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(25f))
            Text("${history.pres_rdng}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(13f))
            Text("${history.consume}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(12f))
            Text("${history.reader}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(20f))
            Text("${history.remarks}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(30f))
        }
    }
}

@Composable
fun PreviewTabContent(viewModel: CustomerViewModel) {
    //Text("Preview Tab Content")
    if (viewModel.reading.isBlank() || viewModel.consumption.isBlank() || viewModel.pesoValue.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please enter and compute a reading to see the preview.")
        }
        return
    }
    val index = viewModel.currentIndex
    val customer = viewModel.customers[index]
    val referenceNo = "007" + customer.srvc_nmbr.substring(0, 8)
    val rawAcct = customer.acct_nmbr
    val formattedAcct = if (rawAcct.length >= 13) {
        "${rawAcct.substring(0, 3)}-${rawAcct.substring(3, 6)}-${rawAcct.substring(6,10)}-${rawAcct.substring(10, 12)}-${rawAcct.substring(12)}"
    } else {
        rawAcct
    }
    var subTotal = 0.00
    val scNumber = customer.srvc_nmbr.substring(0, 8) + "-" + customer.srvc_nmbr.substring(8)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp) // Margin around the paper
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = Color.White,
            shape = RectangleShape,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // ----- LOGO -----
                Image(
                    painter = painterResource(id = com.example.pamanareadandbill.R.drawable.dcwd_pamana_logo_small),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height(80.dp) // Reduced height from 200.dp
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit, // Ensures aspect ratio is maintained within the box
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                )

                // ----- HEADER -----
                Text(
                    text = "PAMANA WATER CORPORATION",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Dagupan City",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Tel. Nos.: 653-2229/0917-8428653",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "TIN# 009-089-959-002",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "STATEMENT OF ACCOUNT",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "FOR THE MONTH OF: MARCH 2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = customer.cssr_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleY = 2f,
                            scaleX = 1f,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                )
                Text(
                    text = customer.cssr_addr,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Text(
                    text = "Reference # : $referenceNo",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Acct.     # : $formattedAcct",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Srvc.Conn.# : $scNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    "-".repeat(32),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Meter. Nmbr : ${customer.mtr_info}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Date From   : ${customer.date_from}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Date To     : 04/03/2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    "-".repeat(32),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                ReceiptRow("Present Reading: ", viewModel.reading)
                ReceiptRow("Previous Reading: ", customer.prev_rdng.toString())
                ReceiptRow("Consumption: ", viewModel.consumption)
                Text(
                    "-".repeat(32),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                ReceiptRow("Basic Charge: ", "%,.2f".format(viewModel.pesoValue.toDouble()))
                ReceiptRow("Add 12% VAT: ", "%,.2f".format(vat))
                ReceiptRow("Sub-Total: ", "%,.2f".format(subTotal), isBold = true)
                runningTotal = runningTotal + subTotal
                if ((customer.s_citizen == "T" || customer.s_citizen == "S") && (viewModel.consumption.toInt() <= 30)) {
                    ReceiptRow("SC Discount (5%): ", "%,.2f".format(discount))
                    runningTotal = runningTotal - discount
                }
                if (customer.amt_arr != 0.0) {
                    ReceiptRow("Curr. Arrears : ", "%,.2f".format(customer.amt_arr))
                    runningTotal = runningTotal + customer.amt_arr
                }
                if (customer.prev_arr > 0.00) {
                    ReceiptRow("Prev. Arrears: ", "%,.2f".format(customer.prev_arr))
                    runningTotal = runningTotal + customer.prev_arr
                }
                if(customer.amt_mat > 0) {
                    ReceiptRow("Materials: ", "%,.2f".format(customer.amt_mat))
                    runningTotal = runningTotal + customer.amt_mat
                }
                if (customer.amt_misc > 0) {
                    ReceiptRow("Misc: ", "%,.2f".format(customer.amt_misc))
                    runningTotal = runningTotal + customer.amt_misc
                }
                if (customer.amt_pdv > 0 ) {
                    ReceiptRow("PDV: ", "%,.2f".format(customer.amt_pdv))
                    runningTotal = runningTotal + customer.amt_pdv
                }
                Text(
                    "-".repeat(32),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                ReceiptRow("TOTAL CHARGES: ", "%,.2f".format(runningTotal), isBold = true, isDoubleHeight = true)
                Text(
                    "TOTAL AFTER",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                ReceiptRow("03/18/2026:", "%,.2f".format(runningTotal + penalty))
                Text("=".repeat(32), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                ReceiptRow("Meter Reader: ", "GRANDY DECIPULO")  // TODO : Add actual reader name
                ReceiptRow("Remarks:", viewModel.selectedFinding)
                Text(
                    text = "Tue, 3 Mar 2026 11:02:19",                         // TODO : Add actual date and time stamp
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "DEVICE SN: R58M22FWZDD",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text("=".repeat(32), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text (
                    text = "PLEASE SETTLE YOUR BALANCE " +
                           "ON OR BEFORE 03/18/2026 " +
                           "TO AVOID DISCONNECTION",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text("=".repeat(32), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text(
                    text = "Payments accepted at Perez, City Hall, " +
                           "and Tambac offices. As well as at " +
                           "Queen Bank, 7-Eleven, GCash, and other " +
                           "ECPay outlets.",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text("-".repeat(32), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text (
                    text = "PAALALA: INGATAN PO ANG BILL NA ITO. " +
                           "This is NOT VALID as and OFFICIAL RECEIPT. " +
                           "Disregard arrears if payment was made.",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}


@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isDoubleHeight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            modifier = if (isDoubleHeight) {
                Modifier.graphicsLayer(scaleY = 2f, transformOrigin = TransformOrigin(0f, 0.5f))
            } else Modifier
        )
        Text(
            text = value,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = if (isDoubleHeight) {
                Modifier.graphicsLayer(scaleY = 2f, transformOrigin = TransformOrigin(1f, 0.5f))
            } else Modifier
        )
    }
}

@Composable
fun LocationTabContent() {
    Text("Location Tab Content")
}

@Composable
fun PictureTabContent() {
    Text("Picture Tab Content")
}

@Composable
fun InputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    searchItem : String
) {

    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },

        title = {
            Text("$searchItem to search:")
        },

        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(searchItem) },
                singleLine = true
            )
        },

        confirmButton = {
            Button(
                onClick = {
                    onConfirm(value)   // RETURN VALUE
                }
            ) {
                Text("OK")
            }
        },

        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}



@Preview(showBackground = true)
@Composable
fun CustomerInfoPreview() {
    PamanaReadandBillTheme() {
        //ReadingTabContent()
    }
}

@Preview(showBackground = true)
@Composable
fun InputDialogPreview() {
    PamanaReadandBillTheme() {
        InputDialog(onDismiss = {}, onConfirm = {}, "Name")
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryTabContentPreview() {

    PamanaReadandBillTheme() {
        //HistoryTabContent(viewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTabContentPreview() {
    PamanaReadandBillTheme() {
        //PreviewTabContent()
    }
}
