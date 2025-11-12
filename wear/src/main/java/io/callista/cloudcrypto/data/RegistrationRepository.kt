package io.callista.cloudcrypto.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling device registration.
 */
class RegistrationRepository(private val context: Context) {

    private val api = RegistrationApiFactory.create()
    private val deviceInfoManager = DeviceInfoManager(context)
    private val fcmTokenManager = FcmTokenManager(context)

    /**
     * Registers the device with the given serial number and IMEI.
     * @param serialNumber User-entered serial number
     * @param imei User-entered IMEI number
     * @return Result containing the registration response or an error.
     */
    suspend fun registerDevice(serialNumber: String, imei: String): Result<RegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceInfo = deviceInfoManager.getDeviceInfo()

                // Get FCM token for push notification targeting
                val fcmToken = fcmTokenManager.getFcmToken()

                val response = api.registerDevice(
                    serialNumber = serialNumber,
                    imei = imei,
                    id = deviceInfo.androidId,
                    fcmToken = fcmToken,
                    deviceModel = deviceInfo.deviceModel,
                    deviceBrand = deviceInfo.deviceBrand,
                    osVersion = deviceInfo.osVersion,
                    nodeId = deviceInfo.nodeId
                )

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Saves the registration status to SharedPreferences.
     */
    suspend fun saveRegistrationStatus(serialNumber: String, imei: String, isRegistered: Boolean) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("registration_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("is_registered", isRegistered)
                putString("serial_number", serialNumber)
                putString("imei", imei)
                putLong("registration_timestamp", System.currentTimeMillis())
                apply()
            }
        }
    }

    /**
     * Gets the current registration status.
     */
    fun getRegistrationStatus(): RegistrationStatus {
        val prefs = context.getSharedPreferences("registration_prefs", Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean("is_registered", false)
        val serialNumber = prefs.getString("serial_number", null)
        val imei = prefs.getString("imei", null)
        val timestamp = prefs.getLong("registration_timestamp", 0L)

        return RegistrationStatus(
            isRegistered = isRegistered,
            serialNumber = serialNumber,
            imei = imei,
            timestamp = timestamp
        )
    }
}

/**
 * Data class representing registration status.
 */
data class RegistrationStatus(
    val isRegistered: Boolean,
    val serialNumber: String?,
    val imei: String?,
    val timestamp: Long
)
