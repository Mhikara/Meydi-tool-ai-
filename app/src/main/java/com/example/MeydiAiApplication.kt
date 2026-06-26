package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class MeydiAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Kernel
        com.example.core.Kernel.install(this)
        
        // Register Core Modules (Database, Security, AI, Network, Subscription)
        com.example.core.Kernel.register(com.example.core.security.SecurityModule())
        com.example.core.Kernel.register(com.example.core.db.DatabaseModule())
        com.example.core.Kernel.register(com.example.core.network.NetworkModule())
        com.example.core.Kernel.register(com.example.subscription.manager.SubscriptionModule())
        com.example.core.Kernel.register(com.example.payment.manager.PaymentModule())
        com.example.core.Kernel.register(com.example.config.FirebaseModule())
        com.example.core.Kernel.register(com.example.centralapi.core.CentralApiModule())
        
        // Initialize AI Orchestrator with default Gemini Provider
        val geminiKey = com.example.api.ApiKeyRegistry.getGeminiKey()
        com.example.core.ai.AiOrchestrator.registerProvider(
            com.example.core.ai.GeminiAiProvider(geminiKey)
        )
        
        // Boot all registered modules
        com.example.core.Kernel.boot()
    }
}
