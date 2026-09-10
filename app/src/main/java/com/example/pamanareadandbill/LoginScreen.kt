package com.example.pamanareadandbill

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pamanareadandbill.ui.theme.PamanaReadandBillTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onExitClick: () -> Unit,
    navController: NavController
) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //------------------------------------------------------
    // Setup the Preferences that will store certain values
    //------------------------------------------------------
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    UserSession.ipAddr = prefs.getString("ip_addr", "") ?: ""
    UserSession.svrPort = prefs.getString("port", "") ?: ""
    UserSession.dbIPAddr = prefs.getString("db_ip_addr", "") ?: ""
    UserSession.dbPort = prefs.getString("db_port", "") ?: ""
    UserSession.dbUser = prefs.getString("db_user", "") ?: ""
    UserSession.dbPassword = prefs.getString("db_password", "") ?: ""
    UserSession.readDate = prefs.getString("read_date", "") ?: ""
    UserSession.dueDate = prefs.getString("due_date", "") ?: ""
    //-------------------------------------------------------

    val deviceTypes = listOf(
        "DPP 250",
        "SEEWOO",
        "UROVO",
        "OTHERS"
    )

    //val database = AppDatabase.getDatabase(context)
    val db = DatabaseProvider.getDatabase(context)

    var selectedDevice by remember { mutableStateOf(deviceTypes[0]) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            // Display logos at the top of the login screen
            DisplayLogos()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PAMANA WATER CORPORATION – DAGUPAN CITY",
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "READ AND BILL SYSTEM V2.00",
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = ""
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {

                OutlinedTextField(
                    value = selectedDevice,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Device Type") },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    deviceTypes.forEach { device ->

                        DropdownMenuItem(
                            text = { Text(device) },
                            onClick = {
                                selectedDevice = device
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Button(
                    onClick = onExitClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("EXIT")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        /*scope.launch(Dispatchers.IO) {
                            populateData(context)
                        }
                        if (username == "onat" && password == "123") {
                            onLoginClick()
                        } else {
                            errorMessage = "Invalid username or password"
                        }*/
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both username and password"
                            return@Button
                        }

                        scope.launch {
                            // Perform database query on IO thread
                            val reader = withContext(Dispatchers.IO) {
                                db.meterReaderDao().getMeterReader(username, password)
                            }

                            val deviceId = Settings.Secure.getString(
                                context.contentResolver,
                                Settings.Secure.ANDROID_ID
                            )

                            if (reader != null) {
                                UserSession.readerId = reader.reader_id
                                UserSession.readerName = reader.reader_name
                                UserSession.deviceId = deviceId

                                onLoginClick()
                            } else {
                                errorMessage = "Invalid username or password"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LOGIN")
                }
            }
        }
    }
}

@Composable
fun DisplayLogos(
    modifier: Modifier = Modifier
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_dcwd),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_pamana_2),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DisplayLogosPreview() {
    PamanaReadandBillTheme {
        DisplayLogos()
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PamanaReadandBillTheme() {
        //LoginScreen(onLoginClick = {}, onExitClick = {})
    }
}
