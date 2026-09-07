package com.example.pamanareadandbill

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(navController: NavController) {
    Column {
        SettingsTab(navController)
    }
}

@Composable
fun SettingsTab(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Date","Server", "Database", "Password")

    val viewModel: SettingsViewModel = viewModel()

    Column {
        TitleBar(stringResource(com.example.pamanareadandbill.R.string.tb_settings))
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
            0 -> DateTabContent()
            1 -> ServerTabContent(viewModel)
            2 -> DatabaseTabContent()
            3 -> PasswordTabContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTabContent() {
    //Text("Date Settings")
    var readDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var isPickingForReadDate by remember { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState()

    //--------------------------------------------------------------------
    // Setup the Preferences that will store certain values such as Dates
    //--------------------------------------------------------------------
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    //--------------------------------------------------------------------

    //--------------------------------------------------------------------
    // Get the values from the Preferences and store it in Sessions
    //--------------------------------------------------------------------
    LaunchedEffect(Unit) {
        readDate = prefs.getString("read_date", "") ?: ""
        dueDate = prefs.getString("due_date", "") ?: ""

        // Also sync to your global session for immediate use elsewhere
        UserSession.readDate = readDate
        UserSession.dueDate = dueDate
    }
    //--------------------------------------------------------------------

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.util.Date(millis)
                        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        val formattedDate = formatter.format(date)

                        if (isPickingForReadDate) readDate = formattedDate else dueDate = formattedDate
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
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

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = readDate,
                    readOnly = true,
                    onValueChange = {

                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = {
                        Text(text = "Read Date", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                )

                Button(
                    onClick = {
                        isPickingForReadDate = true
                        showDatePicker = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Read Date")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dueDate,
                    readOnly = true,
                    onValueChange = {

                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = {
                        Text(text = "Due Date", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                )

                Button(
                    onClick = {
                        isPickingForReadDate = false
                        showDatePicker = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Due Date")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    //-----------------------------------------------
                    // Save Dates to the Preferences
                    //-----------------------------------------------
                    prefs.edit().apply {
                        putString("read_date", readDate)
                        putString("due_date", dueDate)
                        apply()
                    }
                    //-----------------------------------------------

                    UserSession.readDate = readDate
                    UserSession.dueDate = dueDate

                    Toast.makeText(context, "Dates saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Save Date")
            }
        }
    }
}

@Composable
fun ServerTabContent(viewModel : SettingsViewModel ) {
    //Text("Server Settings")
    var ipAddr by remember { mutableStateOf("") }
    var svrPort by remember { mutableStateOf("") }

    ipAddr = viewModel.ipAddr
    svrPort = viewModel.svrPort

    //--------------------------------------------------------------------
    // Setup the Preferences that will store certain values such as Dates
    //--------------------------------------------------------------------
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    //--------------------------------------------------------------------

    //--------------------------------------------------------------------
    // Get the values from the Preferences and store it in Sessions
    //--------------------------------------------------------------------
    LaunchedEffect(Unit) {
        ipAddr = prefs.getString("ip_addr", "") ?: ""
        svrPort = prefs.getString("port", "") ?: ""

        // Also sync to your global session for immediate use elsewhere
        UserSession.ipAddr = ipAddr
        UserSession.svrPort = svrPort
    }
    //--------------------------------------------------------------------

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
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = ipAddr,
                readOnly = false,
                onValueChange = {
                    viewModel.updateIPAddr(it)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "IP Address", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = svrPort,
                readOnly = false,
                onValueChange = {
                    viewModel.updateSvrPort(it)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Port", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    //-----------------------------------------------
                    // Save Server Settings to the Preferences
                    //-----------------------------------------------
                    prefs.edit().apply {
                        putString("ip_addr", ipAddr)
                        putString("port", svrPort)
                        apply()
                    }

                    UserSession.ipAddr = ipAddr
                    UserSession.svrPort = svrPort

                    Toast.makeText(context, "Server settings saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Save Server Settings")
            }

        }
    }
}

@Composable
fun DatabaseTabContent() {
    // Text("Database Settings")
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
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Database IP Address", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Database Port", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Database User", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Database Password", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Save Database Settings")
            }

        }
    }
}

@Composable
fun PasswordTabContent() {
    //Text("Password Settings")

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
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Old Password", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "New Password", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = "",
                readOnly = true,
                onValueChange = {

                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = {
                    Text(text = "Re-Type Password", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Save Password")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DateTabPreview() {
    DateTabContent()
}

@Preview(showBackground = true)
@Composable
fun ServerTabPreview() {
    val viewModel: SettingsViewModel = viewModel()
    ServerTabContent(viewModel)
}

@Preview(showBackground = true)
@Composable
fun DatabaseTabPreview() {
    DatabaseTabContent()
}

@Preview(showBackground = true)
@Composable
fun PasswordTabPreview() {
    PasswordTabContent()
}

