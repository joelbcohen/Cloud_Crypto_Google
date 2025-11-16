package io.callista.cloudcrypto.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit API interface for device registration.
 */
interface RegistrationApi {

    @GET("public/bgc/static-response")
    suspend fun registerDevice(
        @Query("serialNumber") serialNumber: String,
        @Query("id") id: String,
        @Query("fcmToken") fcmToken: String? = null,
        @Query("publicKey") publicKey: String? = null,
        @Query("attestationBlob") attestationBlob: String? = null,
        @Query("deviceModel") deviceModel: String? = null,
        @Query("deviceBrand") deviceBrand: String? = null,
        @Query("osVersion") osVersion: String? = null,
        @Query("nodeId") nodeId: String? = null
    ): RegistrationResponse

    @GET("public/bgc/static-response")
    suspend fun deregisterDevice(
        @Query("action") action: String = "deregister"
    ): RegistrationResponse
}

/**
 * Response from the registration API.
 */
data class RegistrationResponse(
    val status: String? = null,
    val message: String? = null
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
