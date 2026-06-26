package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.security.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = SecurityManager(application)
    private val requestInterceptor = RequestInterceptor(application)

    // State status lingkungan perangkat
    private val _isRooted = MutableStateFlow(false)
    val isRooted: StateFlow<Boolean> = _isRooted.asStateFlow()

    private val _isEmulator = MutableStateFlow(false)
    val isEmulator: StateFlow<Boolean> = _isEmulator.asStateFlow()

    // State status API Key
    private val _apiKeyPreview = MutableStateFlow("Tidak ada API Key")
    val apiKeyPreview: StateFlow<String> = _apiKeyPreview.asStateFlow()

    private val _isApiKeyValid = MutableStateFlow(false)
    val isApiKeyValid: StateFlow<Boolean> = _isApiKeyValid.asStateFlow()

    // State audit log keamanan
    private val _securityLogs = MutableStateFlow<List<SecurityLog>>(emptyList())
    val securityLogs: StateFlow<List<SecurityLog>> = _securityLogs.asStateFlow()

    // State fitur simulasi request
    private val _simulatedResponse = MutableStateFlow("")
    val simulatedResponse: StateFlow<String> = _simulatedResponse.asStateFlow()

    private val _isRequestLoading = MutableStateFlow(false)
    val isRequestLoading: StateFlow<Boolean> = _isRequestLoading.asStateFlow()

    private val _requestError = MutableStateFlow<String?>(null)
    val requestError: StateFlow<String?> = _requestError.asStateFlow()

    // State fitur simulasi enkripsi teks
    private val _plainText = MutableStateFlow("")
    val plainText: StateFlow<String> = _plainText.asStateFlow()

    private val _encryptedText = MutableStateFlow("")
    val encryptedText: StateFlow<String> = _encryptedText.asStateFlow()

    private val _decryptedText = MutableStateFlow("")
    val decryptedText: StateFlow<String> = _decryptedText.asStateFlow()

    init {
        checkDeviceEnvironment()
        refreshSecurityState()
    }

    /**
     * Memeriksa kondisi lingkungan perangkat
     */
    private fun checkDeviceEnvironment() {
        _isRooted.value = securityManager.isDeviceRooted()
        _isEmulator.value = securityManager.isRunningOnEmulator()
    }

    /**
     * Memperbarui state keamanan yang ditampilkan di UI
     */
    fun refreshSecurityState() {
        val key = securityManager.apiKeyManager.getLocalApiKey()
        _apiKeyPreview.value = if (key != null) {
            "${key.take(12)}... [TERENKRIPSI AES-GCM]"
        } else {
            "Tidak ada API Key"
        }
        _isApiKeyValid.value = securityManager.apiKeyManager.isApiKeyValid()
        _securityLogs.value = securityManager.spamProtectionManager.getAuditLogs()
    }

    /**
     * Mensimulasikan pengiriman request aman ke server backend (dilengkapi rate limiter, signature, cache, dan retry)
     */
    fun runSimulatedRequest(method: String, payload: String = "") {
        viewModelScope.launch {
            _isRequestLoading.value = true
            _requestError.value = null
            _simulatedResponse.value = ""
            
            // Catat log permulaan simulasi
            securityManager.spamProtectionManager.logEvent(
                "Memulai Simulasi Request", 
                "INFO", 
                "Melakukan request simulasi dengan metode '$method', Payload: '$payload'"
            )

            // Validasi input payload dari serangan cyber sederhana
            if (payload.isNotEmpty() && !securityManager.validateInputPayload(payload)) {
                _requestError.value = "Kritis: Request dibatalkan. Payload terdeteksi tidak aman (Potensi SQLi/XSS)!"
                _isRequestLoading.value = false
                refreshSecurityState()
                return@launch
            }

            try {
                // Eksekusi request aman yang disaring oleh RequestInterceptor kita
                val response = requestInterceptor.executeSecureRequest(
                    userId = "usr_meydi_dev_99",
                    url = "https://meydi-api.secure-os.internal/v2/compute",
                    method = method,
                    payload = payload
                ) {
                    // Simulasi delay jaringan real-time
                    Thread.sleep(1200)
                    
                    // Simulasi jika terjadi error jaringan acak untuk menguji Auto Retry (peluang 25%)
                    if (Math.random() < 0.25) {
                        throw IOException("503 Service Unavailable: Gangguan jaringan intermiten.")
                    }

                    // Response sukses
                    "{\n  \"status\": \"success\",\n  \"message\": \"Komputasi awan aman berhasil diproses!\",\n  \"server_timestamp\": ${System.currentTimeMillis()},\n  \"data_received\": \"$payload\"\n}"
                }

                _simulatedResponse.value = response
                securityManager.spamProtectionManager.logEvent(
                    "Simulasi Request Sukses", 
                    "INFO", 
                    "Menerima respon dari server: ${response.take(50)}..."
                )
            } catch (e: RateLimitExceededException) {
                _requestError.value = e.message
            } catch (e: DuplicateRequestException) {
                _requestError.value = e.message
            } catch (e: Exception) {
                _requestError.value = e.message ?: "Terjadi kesalahan koneksi yang tidak diketahui."
            } finally {
                _isRequestLoading.value = false
                refreshSecurityState()
            }
        }
    }

    /**
     * Memperbarui batas rate limit secara dinamis (Simulasi remote config update)
     */
    fun updateSpamConfig(maxReq: Int, windowSec: Int) {
        securityManager.updateSpamLimits(maxReq, windowSec * 1000L)
        refreshSecurityState()
    }

    /**
     * Mematikan / Mencabut API Key lokal dengan aman
     */
    fun revokeLocalApiKey() {
        securityManager.apiKeyManager.revokeApiKey()
        refreshSecurityState()
    }

    /**
     * Memaksa pemicuan rotasi API Key baru secara aman
     */
    fun triggerApiKeyRotation() {
        viewModelScope.launch {
            _isRequestLoading.value = true
            val success = securityManager.apiKeyManager.rotateApiKey()
            _isRequestLoading.value = false
            if (success) {
                securityManager.spamProtectionManager.logEvent(
                    "Rotasi API Key Berhasil", 
                    "INFO", 
                    "Rotasi API Key baru berhasil diselesaikan dan disimpan."
                )
            }
            refreshSecurityState()
        }
    }

    /**
     * Mensimulasikan Enkripsi Teks menggunakan kunci Android Keystore
     */
    fun encryptText(text: String) {
        _plainText.value = text
        if (text.isEmpty()) {
            _encryptedText.value = ""
            _decryptedText.value = ""
            return
        }
        try {
            val enc = securityManager.encryptData(text)
            _encryptedText.value = enc
            _decryptedText.value = "" // Reset decrypted
            securityManager.spamProtectionManager.logEvent(
                "Enkripsi Teks Selesai", 
                "INFO", 
                "Teks berhasil dienkripsi menjadi base64 dengan tag otentikasi GCM."
            )
            refreshSecurityState()
        } catch (e: Exception) {
            _encryptedText.value = "Gagal mengenkripsi: ${e.localizedMessage}"
        }
    }

    /**
     * Mensimulasikan Dekripsi Teks terenkripsi dari Android Keystore
     */
    fun decryptText(encryptedBase64: String) {
        if (encryptedBase64.isEmpty()) {
            _decryptedText.value = ""
            return
        }
        try {
            val dec = securityManager.decryptData(encryptedBase64)
            _decryptedText.value = dec
            securityManager.spamProtectionManager.logEvent(
                "Dekripsi Teks Selesai", 
                "INFO", 
                "Teks base64 berhasil didekripsi kembali menjadi teks asli."
            )
            refreshSecurityState()
        } catch (e: Exception) {
            _decryptedText.value = "Gagal mendekripsi: Kemungkinan data rusak atau kunci telah berubah."
        }
    }

    /**
     * Membersihkan seluruh data keamanan (Log, Cache, Key)
     */
    fun clearAllSecurityData() {
        securityManager.spamProtectionManager.reset()
        securityManager.apiKeyManager.revokeApiKey()
        _simulatedResponse.value = ""
        _requestError.value = null
        _plainText.value = ""
        _encryptedText.value = ""
        _decryptedText.value = ""
        refreshSecurityState()
    }
}
