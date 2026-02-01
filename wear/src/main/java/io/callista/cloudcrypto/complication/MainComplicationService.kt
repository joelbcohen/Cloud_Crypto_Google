package io.callista.cloudcrypto.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import io.callista.cloudcrypto.data.RegistrationRepository
import io.callista.cloudcrypto.presentation.MainActivity
import java.text.DecimalFormat

/**
 * Complication data source that displays account balance.
 * Shows "0" when not registered, or the current balance when registered.
 * Tapping the complication opens the Cloud Crypto watch app.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "ComplicationService"

        fun requestUpdate(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, MainComplicationService::class.java)
                )
                requester.requestUpdateAll()
                Log.d(TAG, "Complication update requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request complication update", e)
            }
        }
    }

    private val repository by lazy { RegistrationRepository(applicationContext) }
    private val decimalFormat = DecimalFormat("#,##0.##")

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("1,000", "Balance: 1,000")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val status = repository.getRegistrationStatus()

        if (!status.isRegistered) {
            return createComplicationData("0", "Balance: 0")
        }

        // Fetch live balance from the API
        val balance = try {
            val result = repository.getAccountSummary()
            result.getOrNull()?.data?.balance?.also { repository.saveBalance(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch balance for complication", e)
            null
        }

        // Fall back to cached balance if API call fails
        val balanceStr = balance ?: repository.getBalance()
        val formatted = try {
            decimalFormat.format(balanceStr.toDouble())
        } catch (e: NumberFormatException) {
            balanceStr
        }
        return createComplicationData(formatted, "Balance: $formatted")
    }

    private fun createComplicationData(text: String, contentDescription: String): ComplicationData {
        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapAction = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setTapAction(tapAction)
            .build()
    }
}
