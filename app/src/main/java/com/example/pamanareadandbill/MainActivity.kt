package com.example.pamanareadandbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pamanareadandbill.ui.theme.PamanaReadandBillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PamanaReadandBillTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "login") {

                    composable("login") {
                        LoginScreen(
                            onLoginClick = {
                                //startActivity(Intent(this, DashboardActivity::class.java))
                                navController.navigate("main")
                            },
                            onExitClick = {
                                finishAffinity()
                            },
                            navController)
                    }

                    composable("main") {
                        MainScreen(navController)
                    }

                    composable("read_entry") {
                        ReadEntryScreen(navController)
                    }

                    composable("data") {
                        DataScreen(navController)
                    }

                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PamanaReadandBillTheme {
        Greeting("Android")
    }
}