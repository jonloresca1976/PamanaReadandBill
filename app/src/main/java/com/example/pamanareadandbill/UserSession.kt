package com.example.pamanareadandbill

/**
 * A global singleton to store session data throughout the app's lifecycle.
 */
object UserSession {
    var readerId: String?= null
    var readerName: String? = null
    var deviceId: String? = null
    var readDate: String? = null
    var dueDate: String? = null
    var ipAddr: String? = null
    var svrPort: String? = null

    /**
     * Call this when the user logs out to clear sensitive data.
     */
    fun clear() {
        readerId = null
        readerName = null
        deviceId = null
        readDate = null
        dueDate = null
        ipAddr = null
        svrPort = null

    }
}