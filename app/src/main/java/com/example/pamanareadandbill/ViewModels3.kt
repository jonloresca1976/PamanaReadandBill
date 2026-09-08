package com.example.pamanareadandbill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    var ipAddr  by mutableStateOf("")
    var svrPort by mutableStateOf("")
    var dbIPAddr by mutableStateOf(value = "")
    var dbPort by mutableStateOf(value = "")
    var dbUser by mutableStateOf(value = "")
    var dbPassword by mutableStateOf(value = "")
    var isModified by mutableStateOf(false)  // flag to track if changes has been made

    fun updateIPAddr(value: String) {
        ipAddr = value
        isModified = true
    }

    fun updateSvrPort(value: String) {
        svrPort = value
        isModified = true
    }

    fun updateDBIPAddr(value: String) {
        dbIPAddr = value
        isModified = true
    }

    fun updateDBPort(value: String) {
        dbPort = value
        isModified = true
    }

    fun updateDBUser(value: String) {
        dbUser = value
        isModified = true
    }

    fun updateDBPassword(value: String) {
        dbPassword = value
        isModified = true
    }
}
