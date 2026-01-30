package io.callista.cloudcrypto.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.callista.cloudcrypto.MainActivity
import io.callista.cloudcrypto.R
import io.callista.cloudcrypto.data.FcmTokenManager

/**
 * Firebase Cloud Messaging service for receiving push notifications on mobile.
 *
 * This service handles:
 * - New FCM token registration
 * - Incoming FCM messages with device-specific payloads
 * - Showing notifications to the user
 */
class CloudCryptoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CloudCryptoFCM"
        private const val CHANNEL_ID = "cloud_crypto_fcm"
        private const val NOTIFICATION_ID = 1001
    }

    private val fcmTokenManager by lazy { FcmTokenManager(applicationContext) }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app install, or when the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: ${token.take(10)}...")

        // Save token locally
        fcmTokenManager.saveTokenLocally(token)
    }

    /**
     * Called when a push notification message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "========================================")
        Log.d(TAG, "FCM MESSAGE RECEIVED!")
        Log.d(TAG, "========================================")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")
        Log.d(TAG, "========================================")

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

    private fun handleRegistrationUpdate(data: Map<String, String>) {
        val serialNumber = data["serialNumber"]
        val status = data["status"]
        Log.d(TAG, "Registration update - Serial: $serialNumber, Status: $status")
    }

    private fun handleConfigUpdate(data: Map<String, String>) {
        Log.d(TAG, "Configuration update received")
    }

    private fun handleStatusUpdate(data: Map<String, String>) {
        val status = data["status"]
        Log.d(TAG, "Status update: $status")
    }

    private fun handleGenericMessage(data: Map<String, String>) {
        Log.d(TAG, "Generic message received with ${data.size} data fields")

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
     * Shows a notification when an FCM message is received.
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        try {
            createNotificationChannel()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
