package com.example.payment.manager

import android.content.Context
import com.example.core.AppModule
import com.example.payment.repository.PaymentRepository

class PaymentModule : AppModule {
    override val id: String = "core.payment"
    
    private var _repository: PaymentRepository? = null
    val repository: PaymentRepository get() = _repository ?: throw IllegalStateException("Payment not initialized")

    override fun init(context: Context) {
        _repository = PaymentRepository()
    }

    override fun onShutdown() {
        _repository = null
    }
}
