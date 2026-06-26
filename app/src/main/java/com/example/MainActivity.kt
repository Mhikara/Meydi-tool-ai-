package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MeydiAiApp

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize centralized api key registry with applicationContext
        try { com.example.api.ApiKeyRegistry.init(this) } catch (e: Exception) {}
        
        // Initialize centralized encryption manager
        try { com.example.security.EncryptionManager.initialize(this) } catch (e: Exception) {}

        // Initialize production data management systems
        try { com.example.logging.AppLogger.init(this) } catch (e: Exception) {}
        
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {}

        try { com.example.activity.ActivityLogger.init(this) } catch (e: Exception) {}
        try { com.example.cache.CacheManager.init(this) } catch (e: Exception) {}
        try { com.example.recovery.RecoveryManager.init(this) } catch (e: Exception) {}
        try { com.example.sync.SyncManager.init(this) } catch (e: Exception) {}
        try { com.example.sync.BackgroundSyncWorker.schedulePeriodicSync(this) } catch (e: Exception) {}
        
        // Edge to Edge content padding and status line styling
        enableEdgeToEdge()
        
        setContent {
            // Renders the full MeydiAi Studio application
            MeydiAiApp()
        }
    }
}
