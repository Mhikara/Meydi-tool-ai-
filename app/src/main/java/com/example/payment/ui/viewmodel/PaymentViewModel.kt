package com.example.payment.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.payment.model.PaymentStatus
import com.example.payment.model.PaymentTransaction
import com.example.payment.repository.PaymentRepository
import com.example.subscription.model.SubscriptionPlan
import com.example.subscription.repository.SubscriptionRepository
import com.example.subscription.model.SubscriptionInfo
import com.example.subscription.model.SubscriptionStatus
import com.example.subscription.model.UserRole
import com.example.auth.repository.AuthRepository
import com.example.auth.repository.FirebaseAuthRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val paymentRepo: PaymentRepository,
    private val subRepo: SubscriptionRepository,
    private val authRepo: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    val transaction: StateFlow<PaymentTransaction?> = paymentRepo.currentTransaction
    
    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private var timerJob: Job? = null
    private var selectedPlan: SubscriptionPlan? = null

    fun startPayment(plan: SubscriptionPlan, amount: Double) {
        selectedPlan = plan
        val txn = paymentRepo.createTransaction(amount)
        startTimer(txn.expiresAt)
    }

    private fun startTimer(expiresAt: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (System.currentTimeMillis() < expiresAt) {
                _timeLeft.value = (expiresAt - System.currentTimeMillis()) / 1000
                delay(1000)
            }
            _timeLeft.value = 0
        }
    }

    fun checkStatus() {
        val txn = transaction.value ?: return
        viewModelScope.launch {
            val status = paymentRepo.verifyPayment(txn.id)
            if (status == PaymentStatus.SUCCESS) {
                activatePremium()
            }
        }
    }

    private fun activatePremium() {
        val plan = selectedPlan ?: return
        val startTime = System.currentTimeMillis()
        val durationMs = plan.durationDays.toLong() * 24 * 60 * 60 * 1000
        val endDate = startTime + durationMs
        
        val subInfo = SubscriptionInfo(
            role = UserRole.PREMIUM,
            status = SubscriptionStatus.ACTIVE,
            plan = plan,
            startDate = startTime,
            endDate = endDate,
            autoRenewal = false,
            lastTransactionId = transaction.value?.invoiceId
        )
        subRepo.updateSubscription(subInfo)
        
        // Update Firestore User Doc
        viewModelScope.launch {
            val user = authRepo.currentUser.firstOrNull()
            if (user != null) {
                val updatedUser = user.copy(
                    role = "Premium",
                    isPremium = true
                )
                authRepo.updateProfile(updatedUser)
            }
        }
    }
}
