package com.example.security

import android.net.Uri
import java.net.HttpURLConnection
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

object NetworkSecurity {

    /**
     * Memvalidasi apakah URL tujuan menggunakan protokol HTTPS/TLS yang aman
     */
    fun isConnectionSecure(urlString: String): Boolean {
        return try {
            val uri = Uri.parse(urlString)
            uri.scheme?.lowercase() == "https"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mengonfigurasi HttpURLConnection dengan parameter TLS modern yang aman
     */
    fun configureSecureConnection(connection: HttpURLConnection) {
        if (connection is HttpsURLConnection) {
            try {
                val sslContext = SSLContext.getInstance("TLSv1.3")
                sslContext.init(null, null, null)
                connection.sslSocketFactory = sslContext.socketFactory
                
                // Menerapkan Hostname Verification standar untuk mencegah Man-In-The-Middle (MITM)
                connection.hostnameVerifier = HostnameVerifier { hostname, session ->
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Menangani pesan kesalahan jaringan secara aman (Sanitized Network Error)
     * untuk mencegah kebocoran alamat server internal, API key, atau informasi peka lainnya.
     */
    fun sanitizeNetworkErrorMessage(throwable: Throwable): String {
        val originalMessage = throwable.localizedMessage ?: "Unknown network error"
        return when {
            originalMessage.contains("SSLHandshakeException") || originalMessage.contains("CertPathValidatorException") -> {
                "Kesalahan Validasi Keamanan: Sertifikat SSL server tidak valid atau dicurigai mengalami serangan MITM."
            }
            originalMessage.contains("ConnectException") || originalMessage.contains("UnknownHostException") -> {
                "Koneksi jaringan gagal. Harap periksa sambungan internet Anda."
            }
            originalMessage.contains("401") || originalMessage.contains("Unauthorized") -> {
                "Sesi Anda tidak sah atau token otentikasi telah kedaluwarsa."
            }
            originalMessage.contains("403") || originalMessage.contains("Forbidden") -> {
                "Akses ditolak oleh kebijakan keamanan server."
            }
            else -> {
                "Gangguan jaringan terdeteksi. Silakan coba beberapa saat lagi."
            }
        }
    }
}
