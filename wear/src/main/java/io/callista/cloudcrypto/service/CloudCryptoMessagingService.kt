package io.callista.cloudcrypto.service

import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.callista.cloudcrypto.complication.MainComplicationService
import io.callista.cloudcrypto.data.FcmTokenManager
import io.callista.cloudcrypto.data.RegistrationRepository

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 *
 * This service handles:
 * - New FCM token registration
 * - Incoming FCM messages with device-specific payloads
 * - Updating local storage and complications based on received data
 */
class CloudCryptoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CloudCryptoFCM"
    }

    private val fcmTokenManager by lazy { FcmTokenManager(applicationContext) }
    private val repository by lazy { RegistrationRepository(applicationContext) }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app install, or when the token is refreshed.
     *
     * The token should be sent to your backend server so it can target this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: ${token.take(10)}...")

        // Save token locally
        fcmTokenManager.saveTokenLocally(token)

        // TODO: If device is already registered, send updated token to backend
        // This ensures the backend always has the latest token for this device
    }

    /**
     * Called when a push notification message is received.
     *
     * Expected message data format:
     * {
     *   "type": "registration_update" | "config_update" | "status_update",
     *   "serialNumber": "ABC123",
     *   "status": "active" | "pending" | "expired",
     *   "message": "Your device registration has been confirmed",
     *   "data": { ... additional data ... }
     * }
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")

        val data = remoteMessage.data

        when (data["type"]) {
            "registration_update" -> handleRegistrationUpdate(data)
            "config_update" -> handleConfigUpdate(data)
            "status_update" -> handleStatusUpdate(data)
            else -> {
                Log.w(TAG, "Unknown message type: ${data["type"]}")
                handleGenericMessage(data)
            }
        }

        // Show notification if present
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
            // You can show a notification here if needed
        }
    }

    /**
     * Handles registration update messages from the backend.
     * Updates local storage and complication display.
     */
    private fun handleRegistrationUpdate(data: Map<String, String>) {
        val serialNumber = data["serialNumber"]
        val status = data["status"]
        val message = data["message"]

        Log.d(TAG, "Registration update - Serial: $serialNumber, Status: $status")

        // Update local registration status
        if (serialNumber != null && status == "active") {
            // Save registration confirmation
            // repository.saveRegistrationStatus(serialNumber, true)
        }

        // Update complication to reflect new status
        updateComplication()
    }

    /**
     * Handles configuration update messages.
     */
    private fun handleConfigUpdate(data: Map<String, String>) {
        Log.d(TAG, "Configuration update received")

        // TODO: Update app configuration based on received data
        // Example: update API endpoints, feature flags, etc.

        // Update complication if needed
        updateComplication()
    }

    /**
     * Handles status update messages.
     */
    private fun handleStatusUpdate(data: Map<String, String>) {
        val status = data["status"]
        Log.d(TAG, "Status update: $status")

        // Update complication to show current status
        updateComplication()
    }

    /**
     * Handles generic messages that don't match known types.
     */
    private fun handleGenericMessage(data: Map<String, String>) {
        Log.d(TAG, "Generic message received with ${data.size} data fields")

        // Store in SharedPreferences for later retrieval if needed
        val prefs = getSharedPreferences("fcm_messages", MODE_PRIVATE)
        prefs.edit().apply {
            data.forEach { (key, value) ->
                putString("last_$key", value)
            }
            putLong("last_message_time", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Updates the watch face complication to reflect new data.
     */
    private fun updateComplication() {
        try {
            // Request complication update
            val requester = ComplicationDataSourceUpdateRequester.create(
                applicationContext,
                android.content.ComponentName(
                    applicationContext,
                    MainComplicationService::class.java
                )
            )
            requester.requestUpdateAll()
            Log.d(TAG, "Complication update requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update complication", e)
        }
    }
}
