package com.example.payment.repository

import com.example.payment.model.PaymentStatus
import com.example.payment.model.PaymentTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class PaymentRepository {
    private val _currentTransaction = MutableStateFlow<PaymentTransaction?>(null)
    val currentTransaction: StateFlow<PaymentTransaction?> = _currentTransaction.asStateFlow()

    fun createTransaction(amount: Double): PaymentTransaction {
        val uniqueCode = (100..999).random()
        val timestamp = System.currentTimeMillis()
        val transaction = PaymentTransaction(
            id = UUID.randomUUID().toString(),
            invoiceId = "INV-${timestamp / 1000}",
            transactionCode = "TRX-${(1000..9999).random()}-${timestamp % 10000}",
            amount = amount,
            uniqueCode = uniqueCode
        )
        _currentTransaction.value = transaction
        return transaction
    }

    suspend fun verifyPayment(transactionId: String): PaymentStatus {
        _currentTransaction.value = _currentTransaction.value?.copy(status = PaymentStatus.VERIFYING)
        
        delay(2000) // Simulasi latency verifikasi server
        
        // Dalam realitas, panggil API Backend di sini
        val finalStatus = PaymentStatus.SUCCESS 
        
        _currentTransaction.value = _currentTransaction.value?.copy(status = finalStatus)
        return finalStatus
    }

    fun resetTransaction() {
        _currentTransaction.value = null
    }
}
