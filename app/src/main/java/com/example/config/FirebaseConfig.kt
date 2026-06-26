package com.example.config

data class FirebaseConfig(
    val apiKey: String,
    val applicationId: String,
    val databaseUrl: String? = null,
    val projectId: String,
    val storageBucket: String? = null,
    val gcmSenderId: String? = null
)
