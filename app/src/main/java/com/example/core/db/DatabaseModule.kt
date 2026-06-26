package com.example.core.db

import android.content.Context
import com.example.core.AppModule
import com.example.db.AppDatabase

class DatabaseModule : AppModule {
    override val id: String = "core.database"
    private var _database: AppDatabase? = null
    val database: AppDatabase get() = _database ?: throw IllegalStateException("Database not initialized")

    override fun init(context: Context) {
        _database = AppDatabase.getDatabase(context)
    }

    override fun onShutdown() {
        _database?.close()
        _database = null
    }
}
