package com.example.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface AzbryApi {
    @FormUrlEncoded
    @POST("api/download/allinone")
    suspend fun downloadMedia(
        @Header("x-api-key") apiKey: String,
        @Field("url") url: String
    ): AzbryResponse
}

object AzbryRetrofitClient {
    private const val BASE_URL = "https://api.azbry.com/"

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: AzbryApi by lazy {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
            .build()
        retrofit.create(AzbryApi::class.java)
    }
}

object AzbryDownloader {
    private const val TAG = "AzbryDownloader"

    private fun getApiKey(): String {
        return ApiKeyRegistry.getAzbryKey()
    }
    
    // Catatan: Anda menggunakan URL sebagai API Key di prompt. 
    // Jika 'https://api.azbry.com/api/download/allinone' adalah endpoint-nya, 
    // saya akan pastikan x-api-key tetap terisi atau menggunakan token yang benar jika ada.
    // Namun jika maksud Anda adalah token tertentu, silakan masukkan kodenya di sini.
    // Untuk sementara saya asumsikan x-api-key membutuhkan nilai spesifik.
    suspend fun fetchMedia(url: String): AzbryResponse? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            return@withContext AzbryRetrofitClient.service.downloadMedia(apiKey, url)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Gagal mengunduh media dari Azbry", e)
            return@withContext null
        }
    }
}

data class AzbryResponse(
    val status: Boolean?,
    val result: AzbryResult?
)

data class AzbryResult(
    val title: String?,
    val thumbnail: String?,
    val duration: String?,
    val media: List<AzbryMedia>?
)

data class AzbryMedia(
    val quality: String?,
    val url: String?,
    val type: String?, // video, audio, image
    val extension: String?
)
