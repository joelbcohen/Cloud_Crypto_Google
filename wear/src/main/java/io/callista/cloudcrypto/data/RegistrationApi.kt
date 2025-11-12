package io.callista.cloudcrypto.data

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
        @Query("imei") imei: String,
        @Query("id") id: String,
        @Query("deviceModel") deviceModel: String? = null,
        @Query("deviceBrand") deviceBrand: String? = null,
        @Query("osVersion") osVersion: String? = null
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

    fun create(): RegistrationApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
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
