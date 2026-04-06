package com.example.pamanareadandbill

import android.R
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
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
    val tabs = listOf("Reading", "History", "Location", "Picture")

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
            2 -> LocationTabContent()
            3 -> PictureTabContent()
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
                    text = "P/R: $prevReading",
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
                        val cons = r - prevReading
                        viewModel.updateConsumption(cons.toString()) // Update ViewModel
                        computeValue(cons, customers[index].acct_nmbr, viewModel) // Update ViewModel
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

            if (showDialog) {

                InputDialog(

                    onDismiss = {
                        showDialog = false
                    },

                    onConfirm = { value ->
                        returnedValue = value   // RECEIVE VALUE
                        showDialog = false
                    },
                    searchItem = itemToSearch
                )
            }

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
                        viewModel.previous()
                        clearFields(viewModel)
                        averagePrefix=""
                        scope.launch {
                            loadReadings(viewModel, db)
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
                            //val errorEncountered = averagePrefix != "" && selectedFinding == ""
                            val isUsingAverage = averagePrefix.isNotEmpty()
                            // Check if finding is empty, blank, or matches your default "empty" string
                            val noFindingSelected = selectedFinding.trim().isEmpty() || selectedFinding == "                "
                            if (isUsingAverage && noFindingSelected) {
                                errorDesc = "Please select a finding"
                                showErrorDialog = true
                                return@launch // Exit the coroutine
                            }
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
                        viewModel.next()
                        clearFields(viewModel)
                        averagePrefix=""
                        scope.launch {
                            loadReadings(viewModel, db)
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
