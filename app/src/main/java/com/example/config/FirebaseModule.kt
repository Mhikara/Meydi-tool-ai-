package com.example.config

import android.content.Context
import com.example.core.AppModule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class FirebaseModule : AppModule {
    override val id: String = "FirebaseModule"

    override fun init(context: Context) {
        val configManager = FirebaseConfigManager(context)
        configManager.initializeFirebase()
    }
}
