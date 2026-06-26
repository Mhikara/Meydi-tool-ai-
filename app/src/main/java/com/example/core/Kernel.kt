package com.example.core

import android.content.Context
import android.util.Log

interface AppModule {
    val id: String
    fun init(context: Context)
    fun onShutdown() {}
}

object Kernel {
    private val modules = mutableMapOf<String, AppModule>()
    private var isInitialized = false
    private lateinit var appContext: Context

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    fun register(module: AppModule) {
        modules[module.id] = module
        if (isInitialized) {
            module.init(appContext)
        }
    }

    fun boot() {
        if (isInitialized) return
        Log.d("Kernel", "Booting Core System...")
        modules.values.forEach { 
            Log.d("Kernel", "Initializing module: ${it.id}")
            it.init(appContext) 
        }
        isInitialized = true
        Log.d("Kernel", "Core System Ready.")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : AppModule> get(id: String): T? = modules[id] as? T
}
