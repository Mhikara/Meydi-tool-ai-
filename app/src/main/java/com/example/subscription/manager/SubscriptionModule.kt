package com.example.subscription.manager

import android.content.Context
import com.example.core.AppModule
import com.example.subscription.repository.SubscriptionRepository

class SubscriptionModule : AppModule {
    override val id: String = "core.subscription"
    
    private var _repository: SubscriptionRepository? = null
    val repository: SubscriptionRepository get() = _repository ?: throw IllegalStateException("Subscription not initialized")

    override fun init(context: Context) {
        _repository = SubscriptionRepository()
        // Load initial data from SecurePrefs or Database
    }

    override fun onShutdown() {
        _repository = null
    }
}
