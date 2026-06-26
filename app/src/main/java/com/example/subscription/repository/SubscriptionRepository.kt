package com.example.subscription.repository

import com.example.subscription.model.SubscriptionInfo
import com.example.subscription.model.SubscriptionStatus
import com.example.subscription.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionRepository {
    private val _subscriptionInfo = MutableStateFlow(SubscriptionInfo())
    val subscriptionInfo: StateFlow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    fun updateSubscription(info: SubscriptionInfo) {
        _subscriptionInfo.value = info
        // Di sini nantinya bisa ditambahkan logika untuk menyimpan ke DataStore atau Room
    }

    suspend fun checkSubscriptionStatus() {
        // Logika verifikasi ke server atau lokal
        val current = _subscriptionInfo.value
        if (current.endDate != null && current.endDate < System.currentTimeMillis()) {
            _subscriptionInfo.value = current.copy(
                role = UserRole.FREE,
                status = SubscriptionStatus.EXPIRED
            )
        }
    }

    fun hasAccess(featureId: String): Boolean {
        // Logika pengecekan akses fitur berdasarkan role
        if (_subscriptionInfo.value.isPremiumActive) return true
        
        val freeFeatures = listOf("basic_ai", "standard_download", "basic_theme")
        return freeFeatures.contains(featureId)
    }
}
