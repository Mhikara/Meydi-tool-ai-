package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.security.EmailVerificationService
import com.example.security.VerificationData
import com.example.security.VerificationStatusConstants
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val service = EmailVerificationService()
    private val networkMonitor = NetworkMonitor(application)

    private val _verificationData = MutableStateFlow<VerificationData?>(null)
    val verificationData: StateFlow<VerificationData?> = _verificationData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var pollJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        // Pantau status koneksi internet secara real-time
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
            }
        }
    }

    // Melakukan pengecekan status manual (saat tombol ditekan atau onResume)
    fun checkStatus(onVerified: () -> Unit = {}) {
        if (!_isOnline.value) {
            _toastMessage.value = "Koneksi internet tidak tersedia. Menampilkan data offline."
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            val (isVerified, data) = service.checkVerificationStatus()
            _verificationData.value = data
            _isRefreshing.value = false

            if (isVerified) {
                stopAutoPoll()
                onVerified()
            }
        }
    }

    // Mengirim ulang email verifikasi
    fun resendVerificationEmail() {
        if (!_isOnline.value) {
            _toastMessage.value = "Gagal mengirim email: Tidak ada koneksi internet!"
            return
        }

        if (_cooldownSeconds.value > 0) {
            _toastMessage.value = "Silakan tunggu ${_cooldownSeconds.value} detik sebelum mengirim ulang."
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            val success = service.sendVerificationEmail()
            _isRefreshing.value = false

            if (success) {
                _toastMessage.value = "Email verifikasi berhasil dikirim ulang!"
                // Segera muat ulang data lokal
                val currentUid = service.getVerificationInfo(com.example.utils.FirebaseManager.auth?.currentUser?.uid ?: "")
                _verificationData.value = currentUid
                startCooldown()
            } else {
                _toastMessage.value = "Gagal mengirim email verifikasi. Batas pengiriman harian Firebase mungkin terlampaui."
            }
        }
    }

    // Memulai hitung mundur masa cooldown 60 detik
    private fun startCooldown() {
        cooldownJob?.cancel()
        _cooldownSeconds.value = 60
        cooldownJob = viewModelScope.launch {
            while (_cooldownSeconds.value > 0) {
                delay(1000)
                _cooldownSeconds.value -= 1
            }
        }
    }

    // Memulai sinkronisasi berkala (Polling) untuk mendeteksi perubahan status secara real-time
    fun startAutoPoll(onVerified: () -> Unit) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Polling setiap 5 detik
                if (_isOnline.value) {
                    val (isVerified, data) = service.checkVerificationStatus()
                    _verificationData.value = data
                    if (isVerified) {
                        _toastMessage.value = "Email Berhasil Diverifikasi secara Otomatis!"
                        onVerified()
                        break
                    }
                }
            }
        }
    }

    // Menghentikan polling berkala
    fun stopAutoPoll() {
        pollJob?.cancel()
        pollJob = null
    }

    // Menghapus pesan Toast setelah ditampilkan di UI
    fun clearToastMessage() {
        _toastMessage.value = null
    }

    // Fungsi logout jika user ingin kembali ke halaman Login
    fun logout(onLoggedOut: () -> Unit) {
        stopAutoPoll()
        service.logout()
        onLoggedOut()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoPoll()
        cooldownJob?.cancel()
    }
}
