package io.callista.cloudcrypto.complication

import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import io.callista.cloudcrypto.data.RegistrationRepository

/**
 * Complication data source that displays device registration status.
 * Shows "REG" when registered, "---" when not registered.
 * Can be updated when FCM messages are received.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { RegistrationRepository(applicationContext) }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("REG", "Registered")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val status = repository.getRegistrationStatus()

        return if (status.isRegistered) {
            createComplicationData("REG", "Device Registered")
        } else {
            createComplicationData("---", "Not Registered")
        }
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()

    companion object {
        /**
         * Updates the complication data.
         * Call this method when receiving FCM messages to update the complication display.
         */
        fun requestUpdate(context: Context) {
            // This can be called from FCM receiver to update the complication
            // val componentName = ComponentName(context, MainComplicationService::class.java)
            // ProviderUpdateRequester(context, componentName).requestUpdateAll()
        }
    }
}