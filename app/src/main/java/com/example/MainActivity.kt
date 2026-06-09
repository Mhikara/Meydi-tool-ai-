package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MeydiAiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to Edge content padding and status line styling
        enableEdgeToEdge()
        
        setContent {
            // Renders the full MeydiAi Studio application
            MeydiAiApp()
        }
    }
}
