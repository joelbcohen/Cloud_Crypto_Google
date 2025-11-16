package io.callista.cloudcrypto.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling device registration.
 */
class RegistrationRepository(private val context: Context) {

    companion object {
        private const val TAG = "RegistrationRepository"
    }

    private val api = RegistrationApiFactory.create()
    private val deviceInfoManager = DeviceInfoManager(context)
    private val fcmTokenManager = FcmTokenManager(context)
    private val attestationManager = DeviceAttestationManager(context)

    /**
     * Registers the device with the given serial number.
     * @param serialNumber User-entered serial number
     * @return Result containing the registration response or an error.
     */
    suspend fun registerDevice(serialNumber: String): Result<RegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceInfo = deviceInfoManager.getDeviceInfo()

                // Get FCM token for push notification targeting
                val fcmToken = fcmTokenManager.getFcmToken()

                // Generate device attestation data with public key
                Log.d(TAG, "Generating device attestation...")
                val attestationData = attestationManager.generateAttestationData()

                // Log all registration parameters
                Log.d(TAG, "=== Registration Parameters ===")
                Log.d(TAG, "Serial Number: $serialNumber")
                Log.d(TAG, "Android ID: ${deviceInfo.androidId}")
                Log.d(TAG, "FCM Token: ${fcmToken?.take(20)}...${fcmToken?.takeLast(10) ?: "NULL"}")
                Log.d(TAG, "FCM Token Length: ${fcmToken?.length ?: 0}")
                Log.d(TAG, "Public Key: ${attestationData.publicKey.take(50)}...")
                Log.d(TAG, "Public Key Length: ${attestationData.publicKey.length}")
                Log.d(TAG, "Attestation Blob Length: ${attestationData.attestationBlob.length}")
                Log.d(TAG, "Key Algorithm: ${attestationData.algorithm}")
                Log.d(TAG, "Device Model: ${deviceInfo.deviceModel}")
                Log.d(TAG, "Device Brand: ${deviceInfo.deviceBrand}")
                Log.d(TAG, "OS Version: ${deviceInfo.osVersion}")
                Log.d(TAG, "Node ID: ${deviceInfo.nodeId}")
                Log.d(TAG, "===============================")

                // Warn if FCM token is missing
                if (fcmToken.isNullOrBlank()) {
                    Log.w(TAG, "⚠️ WARNING: FCM Token is null or empty! Push notifications will not work.")
                    Log.w(TAG, "Make sure google-services.json is properly configured.")
                }

                val response = api.registerDevice(
                    serialNumber = serialNumber,
                    id = deviceInfo.androidId,
                    fcmToken = fcmToken,
                    publicKey = attestationData.publicKey,
                    attestationBlob = attestationData.attestationBlob.ifBlank { null },
                    deviceModel = deviceInfo.deviceModel,
                    deviceBrand = deviceInfo.deviceBrand,
                    osVersion = deviceInfo.osVersion,
                    nodeId = deviceInfo.nodeId
                )

                Log.d(TAG, "Registration successful: ${response.status}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Deregisters the device.
     * @return Result containing the deregistration response or an error.
     */
    suspend fun deregisterDevice(): Result<RegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deregistering device...")
                val response = api.deregisterDevice()
                Log.d(TAG, "Deregistration successful: ${response.status}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Deregistration failed", e)
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
