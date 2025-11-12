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

    /**
     * Registers the device with the given serial number.
     * @return Result containing the registration response or an error.
     */
    suspend fun registerDevice(serialNumber: String): Result<RegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceInfo = deviceInfoManager.getDeviceInfo()

                val response = api.registerDevice(
                    serialNumber = serialNumber,
                    imei = deviceInfo.deviceIdentifier,
                    id = deviceInfo.androidId,
                    deviceModel = deviceInfo.deviceModel,
                    deviceBrand = deviceInfo.deviceBrand,
                    osVersion = deviceInfo.osVersion
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
    suspend fun saveRegistrationStatus(serialNumber: String, isRegistered: Boolean) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("registration_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("is_registered", isRegistered)
                putString("serial_number", serialNumber)
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
        val timestamp = prefs.getLong("registration_timestamp", 0L)

        return RegistrationStatus(
            isRegistered = isRegistered,
            serialNumber = serialNumber,
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
    val timestamp: Long
)
