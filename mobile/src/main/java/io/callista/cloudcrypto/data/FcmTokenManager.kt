package io.callista.cloudcrypto.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Cloud Messaging (FCM) token for push notifications.
 * This token is sent to the backend during registration so the server can target
 * this specific device with FCM messages.
 */
class FcmTokenManager(private val context: Context) {

    companion object {
        private const val TAG = "FcmTokenManager"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    /**
     * Gets the current FCM registration token.
     * This token uniquely identifies this device/app installation for push notifications.
     */
    suspend fun getFcmToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token retrieved: ${token?.take(10)}...")

            // Save token to SharedPreferences for quick access
            saveTokenLocally(token)

            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            // Return cached token if available
            getCachedToken()
        }
    }

    /**
     * Gets the cached FCM token from SharedPreferences.
     */
    fun getCachedToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Saves the FCM token to SharedPreferences.
     */
    fun saveTokenLocally(token: String?) {
        if (token != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
            Log.d(TAG, "FCM token saved locally")
        }
    }

    /**
     * Deletes the cached FCM token (for logout/reset scenarios).
     */
    fun clearToken() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_FCM_TOKEN).apply()

        // Also delete the token from Firebase
        FirebaseMessaging.getInstance().deleteToken()
        Log.d(TAG, "FCM token cleared")
    }
}
