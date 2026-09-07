package com.example.pamanareadandbill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    var ipAddr  by mutableStateOf("")
    var svrPort by mutableStateOf("")
    var isModified by mutableStateOf(false)  // flag to track if changes has been made

    fun updateIPAddr(value: String) {
        ipAddr = value
        isModified = true
    }

    fun updateSvrPort(value: String) {
        svrPort = value
        isModified = true
    }

}