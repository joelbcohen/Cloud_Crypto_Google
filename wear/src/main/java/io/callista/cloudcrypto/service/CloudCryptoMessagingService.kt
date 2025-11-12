package io.callista.cloudcrypto.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.callista.cloudcrypto.R
import io.callista.cloudcrypto.complication.MainComplicationService
import io.callista.cloudcrypto.data.FcmTokenManager
import io.callista.cloudcrypto.data.RegistrationRepository
import io.callista.cloudcrypto.presentation.MainActivity

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 *
 * This service handles:
 * - New FCM token registration
 * - Incoming FCM messages with device-specific payloads
 * - Updating local storage and complications based on received data
 * - Waking up the watch and showing notifications
 */
class CloudCryptoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CloudCryptoFCM"
        private const val CHANNEL_ID = "cloud_crypto_fcm"
        private const val NOTIFICATION_ID = 1001
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

        // Wake up the watch screen
        wakeUpScreen()

        val data = remoteMessage.data
        val message = data["message"] ?: "Cloud Crypto Notification"
        val type = data["type"] ?: "unknown"

        // Show notification to user
        showNotification(type, message, data)

        when (data["type"]) {
            "registration_update" -> handleRegistrationUpdate(data)
            "config_update" -> handleConfigUpdate(data)
            "status_update" -> handleStatusUpdate(data)
            else -> {
                Log.w(TAG, "Unknown message type: ${data["type"]}")
                handleGenericMessage(data)
            }
        }

        // Show notification if present in FCM notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
            showNotification(
                notification.title ?: "Cloud Crypto",
                notification.body ?: "",
                data
            )
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

    /**
     * Wakes up the watch screen when FCM message is received.
     */
    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "CloudCrypto::FCMWakeLock"
            )
            wakeLock.acquire(3000L) // 3 seconds
            Log.d(TAG, "Watch screen woken up")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wake up screen", e)
        }
    }

    /**
     * Shows a notification on the watch when FCM message is received.
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        try {
            createNotificationChannel()

            // Intent to open the app when notification is tapped
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Add message data to intent extras
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "Notification shown: $title - $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }

    /**
     * Creates notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cloud Crypto Messages"
            val descriptionText = "Notifications for device registration and updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
