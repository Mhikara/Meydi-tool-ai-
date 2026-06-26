package com.example.core.network

import android.content.Context
import com.example.core.AppModule
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkModule : AppModule {
    override val id: String = "core.network"
    
    private var _okHttpClient: OkHttpClient? = null
    val okHttpClient: OkHttpClient get() = _okHttpClient ?: throw IllegalStateException("Network not initialized")

    override fun init(context: Context) {
        _okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-App-Platform", "Android")
                    .addHeader("X-App-Kernel-Version", "1.0.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
