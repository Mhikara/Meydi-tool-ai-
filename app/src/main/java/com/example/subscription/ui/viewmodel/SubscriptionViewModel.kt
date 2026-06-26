package com.example.subscription.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.model.SubscriptionInfo
import com.example.subscription.model.SubscriptionPlan
import com.example.subscription.model.SubscriptionStatus
import com.example.subscription.model.UserRole
import com.example.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    val subscriptionInfo: StateFlow<SubscriptionInfo> = repository.subscriptionInfo

    fun purchasePlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val durationMs = plan.durationDays.toLong() * 24 * 60 * 60 * 1000
            val endTime = startTime + durationMs

            val newInfo = SubscriptionInfo(
                role = UserRole.PREMIUM,
                status = SubscriptionStatus.ACTIVE,
                plan = plan,
                startDate = startTime,
                endDate = endTime,
                autoRenewal = true,
                lastTransactionId = "TXN-${System.currentTimeMillis()}"
            )

            repository.updateSubscription(newInfo)
        }
    }
}
