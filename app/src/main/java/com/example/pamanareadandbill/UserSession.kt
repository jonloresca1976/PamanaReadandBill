package com.example.pamanareadandbill

/**
 * A global singleton to store session data throughout the app's lifecycle.
 */
object UserSession {
    var readerId: String?= null
    var readerName: String? = null
    var deviceId: String? = null

    /**
     * Call this when the user logs out to clear sensitive data.
     */
    fun clear() {
        readerId = null
        readerName = null
        deviceId = null
    }
}