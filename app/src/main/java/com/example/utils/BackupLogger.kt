package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: Long,
        val type: String, // "INFO", "SUCCESS", "ERROR", "SYNC"
        val message: String
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }

    fun log(type: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), type, message)
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry) // Prepend to show latest logs first
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex) // Maintain a limit of 50 logs
        }
        _logs.value = currentList
        println("[BackupLogger] [${entry.type}] ${entry.message}")
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
