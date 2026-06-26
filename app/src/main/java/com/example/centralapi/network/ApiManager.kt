package com.example.centralapi.network

import android.content.Context
import android.util.Log
import com.example.centralapi.api.CentralApiService
import com.example.centralapi.domain.UserRole
import com.example.centralapi.security.CentralSecurityManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiManager(
    private val context: Context,
    private val securityManager: CentralSecurityManager
) {
    private var retrofit: Retrofit? = null
    private var apiService: CentralApiService? = null
    private var currentBaseUrl = "https://meydi-api.web.app/" // Default secure HTTPS API endpoint

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    init {
        rebuildRetrofit()
    }

    /**
     * Rebuilds Retrofit instance. Useful if base URL or configurations are dynamically changed at runtime.
     */
    fun rebuildRetrofit(newBaseUrl: String? = null) {
        if (newBaseUrl != null) {
            currentBaseUrl = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HeaderAndSecurityInterceptor(securityManager))
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(SafeLoggingInterceptor())
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit?.create(CentralApiService::class.java)
        Log.d("ApiManager", "Retrofit initialized with baseUrl: $currentBaseUrl")
    }

    fun getService(): CentralApiService {
        return apiService ?: throw IllegalStateException("ApiManager not initialized correctly")
    }

    fun getBaseUrl(): String = currentBaseUrl

    /**
     * Pre-request security validation checking if user has permission.
     * Prevents calling unauthorized endpoints before they even hit the network!
     */
    fun validateAccess(requiredRole: UserRole): Boolean {
        val currentRole = securityManager.getUserRole()
        val isValid = when (requiredRole) {
            UserRole.USER -> true // Everyone can access USER level
            UserRole.PREMIUM -> currentRole == UserRole.PREMIUM || currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER
            UserRole.ADMIN -> currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER
            UserRole.OWNER -> currentRole == UserRole.OWNER
        }
        if (!isValid) {
            Log.e("ApiManager", "ACCESS DENIED: Required role $requiredRole, but user is $currentRole")
        }
        return isValid
    }

    // --- INTERCEPTORS ---

    /**
     * Interceptor to inject dynamic security headers securely.
     */
    private class HeaderAndSecurityInterceptor(
        private val securityManager: CentralSecurityManager
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            // 1. Inject application wide API Key
            securityManager.getAppApiKey()?.let { apiKey ->
                requestBuilder.addHeader("X-API-Key", apiKey)
            }

            // 2. Inject user authentication JWT token if logged in
            securityManager.getUserToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            // 3. Platform metadata
            requestBuilder.addHeader("X-Platform", "Android")
            requestBuilder.addHeader("X-App-Role", securityManager.getUserRole().name)

            return chain.proceed(requestBuilder.build())
        }
    }

    /**
     * Robust Retry Interceptor for automatic backoff.
     */
    private class RetryInterceptor(private val maxRetries: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response = chain.proceed(request)
            var tryCount = 0

            while (!response.isSuccessful && tryCount < maxRetries) {
                // Retry only for transient failures like 5xx Server Errors or network exceptions
                val code = response.code
                if (code == 503 || code == 504 || code == 502) {
                    tryCount++
                    Log.w("ApiManager", "Transient failure ($code). Retrying... ($tryCount/$maxRetries)")
                    response.close()
                    // Linear backoff
                    try {
                        Thread.sleep(1000L * tryCount)
                    } catch (e: InterruptedException) {
                        break
                    }
                    response = chain.proceed(request)
                } else {
                    break
                }
            }
            return response
        }
    }

    /**
     * Secure Logging Interceptor that hides/masks sensitive information like tokens,
     * passwords, and API keys so they are never printed in Logcat.
     */
    private class SafeLoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            val method = request.method

            Log.i("ApiManager-HTTP", "--> $method $url")
            
            val t1 = System.nanoTime()
            val response: Response
            try {
                response = chain.proceed(request)
            } catch (e: IOException) {
                Log.e("ApiManager-HTTP", "<-- HTTP FAILED: $e")
                throw e
            }
            val t2 = System.nanoTime()
            
            val durationMs = TimeUnit.NANOSECONDS.toMillis(t2 - t1)
            val code = response.code
            val isSuccess = response.isSuccessful

            if (isSuccess) {
                Log.i("ApiManager-HTTP", "<-- $code ${response.message} ($durationMs ms) for $url")
            } else {
                Log.e("ApiManager-HTTP", "<-- FAILED $code ${response.message} ($durationMs ms) for $url")
            }

            return response
        }
    }
}
