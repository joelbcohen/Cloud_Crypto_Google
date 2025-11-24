package io.callista.cloudcrypto.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Manages device identification information for registration.
 * Collects Android ID, device model, and Wearable Node ID as unique identifiers.
 */
class DeviceInfoManager(private val context: Context) {

    /**
     * Gets the Android ID which is unique per device and app installation.
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * Gets the device model and manufacturer information.
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER}-${Build.MODEL}"
    }

    /**
     * Gets the device brand.
     */
    fun getDeviceBrand(): String {
        return Build.BRAND
    }

    /**
     * Gets the Wearable Node ID which uniquely identifies this watch.
     * This is asynchronous as it requires communication with Google Play Services.
     */
    suspend fun getWearableNodeId(): String {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val localNode = nodeClient.localNode.await()
            localNode.id
        } catch (e: Exception) {
            "node-unavailable"
        }
    }

    /**
     * Gets a composite device identifier that combines multiple device identifiers.
     * This provides a unique identifier for the watch device.
     */
    suspend fun getDeviceIdentifier(): String {
        val androidId = getAndroidId()
        val nodeId = getWearableNodeId()
        return "$androidId-$nodeId"
    }

    /**
     * Checks if the app is running on an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Gets the APNS environment based on whether running on emulator or physical device.
     * Returns 'sandbox' for emulators and 'production' for physical devices.
     */
    fun getApnsEnvironment(): String {
        return if (isEmulator()) "sandbox" else "production"
    }

    /**
     * Gets all device information as a map for registration.
     */
    suspend fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            androidId = getAndroidId(),
            deviceModel = getDeviceModel(),
            deviceBrand = getDeviceBrand(),
            nodeId = getWearableNodeId(),
            deviceIdentifier = getDeviceIdentifier(),
            osVersion = Build.VERSION.SDK_INT.toString(),
            apnsEnvironment = getApnsEnvironment()
        )
    }
}

/**
 * Data class representing device information.
 */
data class DeviceInfo(
    val androidId: String,
    val deviceModel: String,
    val deviceBrand: String,
    val nodeId: String,
    val deviceIdentifier: String,
    val osVersion: String,
    val apnsEnvironment: String
)
