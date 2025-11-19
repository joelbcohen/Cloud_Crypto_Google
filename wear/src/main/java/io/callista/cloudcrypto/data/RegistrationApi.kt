package io.callista.cloudcrypto.data

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Retrofit API interface for device registration.
 */
interface RegistrationApi {

    @POST("public/crypto/register")
    suspend fun registerDevice(
        @Body request: RegistrationRequest
    ): RegistrationResponse

    @POST("public/crypto/deregister")
    suspend fun deregisterDevice(
        @Body request: DeregistrationRequest
    ): RegistrationResponse

    @POST("public/crypto/account_summary")
    suspend fun getAccountSummary(
        @Body request: AccountSummaryRequest
    ): AccountSummaryResponse

    @POST("public/crypto/transfer")
    suspend fun transfer(
        @Body request: TransferRequest
    ): TransferResponse
}

/**
 * Request body for device deregistration.
 */
data class DeregistrationRequest(
    val publicKey: String,
    val attestationBlob: String,
    val serialNumber: String
)

/**
 * Request body for device registration.
 */
data class RegistrationRequest(
    val serialNumber: String,
    val id: String,
    val fcmToken: String? = null,
    val publicKey: String? = null,
    val attestationBlob: String? = null,
    val deviceModel: String? = null,
    val deviceBrand: String? = null,
    val osVersion: String? = null,
    val nodeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Response from the registration API.
 */
data class RegistrationResponse(
    val status: String? = null,
    val message: String? = null,
    val registrationId: String? = null,
    val publicKey: String? = null,
    val accountId: String? = null,
    val remainingBalance: Double? = null
)

/**
 * Request body for account summary.
 */
data class AccountSummaryRequest(
    val serialNumber: String,
    val publicKey: String,
    val attestationBlob: String
)

/**
 * Response from the account summary API.
 * Matches the account_summary MySQL view structure.
 */
data class AccountSummaryResponse(
    val status: String? = null,
    val message: String? = null,
    @SerializedName("account")
    val data: AccountSummaryData? = null
)

/**
 * Request body for transfer.
 */
data class TransferRequest(
    val serialNumber: String,
    val publicKey: String,
    val attestationBlob: String,
    val toAccountId: String,
    val amount: String
)

/**
 * Response from the transfer API.
 */
data class TransferResponse(
    val status: String? = null,
    val message: String? = null,
    val transactionId: String? = null,
    val newBalance: String? = null
)

/**
 * Account summary data from the database view.
 * Uses @SerializedName to map snake_case JSON fields to camelCase Kotlin properties.
 */
data class AccountSummaryData(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("balance")
    val balance: String? = null,

    @SerializedName("serial_number")
    val serialNumber: String? = null,

    @SerializedName("serial_hash")
    val serialHash: String? = null,

    @SerializedName("model")
    val model: String? = null,

    @SerializedName("brand")
    val brand: String? = null,

    @SerializedName("os_version")
    val osVersion: String? = null,

    @SerializedName("node_id")
    val nodeId: String? = null,

    @SerializedName("total_sent_transactions")
    val totalSentTransactions: Int = 0,

    @SerializedName("total_received_transactions")
    val totalReceivedTransactions: Int = 0,

    @SerializedName("total_sent_amount")
    val totalSentAmount: String? = null,

    @SerializedName("total_received_amount")
    val totalReceivedAmount: String? = null,

    @SerializedName("account_created_at")
    val accountCreatedAt: String? = null,

    @SerializedName("last_activity")
    val lastActivity: String? = null
)

/**
 * Factory object to create the Retrofit service.
 */
object RegistrationApiFactory {

    private const val BASE_URL = "https://fusio.callista.io/"
    private const val TAG = "RegistrationApi"

    fun create(): RegistrationApi {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Add custom interceptor to log full URL with query parameters
        val urlLoggingInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            Log.d(TAG, "🌐 Full Request URL: $url")

            // Check if fcmToken is in the URL
            if (!url.contains("fcmToken=") || url.contains("fcmToken=&") || url.contains("fcmToken=null")) {
                Log.w(TAG, "⚠️ WARNING: fcmToken is missing or null in the request URL!")
            } else {
                Log.d(TAG, "✅ fcmToken is present in the request")
            }

            // Check for publicKey
            if (url.contains("publicKey=") && !url.contains("publicKey=&") && !url.contains("publicKey=null")) {
                Log.d(TAG, "✅ publicKey is present in the request")
            } else {
                Log.w(TAG, "⚠️ publicKey is missing or null")
            }

            // Check for attestationBlob
            if (url.contains("attestationBlob=") && !url.contains("attestationBlob=&") && !url.contains("attestationBlob=null")) {
                Log.d(TAG, "✅ attestationBlob is present in the request")
            } else {
                Log.w(TAG, "⚠️ attestationBlob is missing or null")
            }

            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(urlLoggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(RegistrationApi::class.java)
    }
}
