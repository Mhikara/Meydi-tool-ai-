package com.example.logging

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.db.AppDatabase
import com.example.security.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream

object AppLogger {
    private const val TAG = "MeydiAppLogger"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1 MB
    private const val LOG_FILE_NAME = "meydiai_system_logs.txt"
    private const val ROTATED_LOG_FILE_NAME = "meydiai_system_logs_old.txt"

    private var applicationContext: Context? = null
    private var database: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var sessionId = UUID.randomUUID().toString()

    fun init(context: Context) {
        val appCtx = context.applicationContext
        applicationContext = appCtx
        database = AppDatabase.getDatabase(appCtx)
        info("AppLogger", "init", "System logger initialized. Session ID: $sessionId")
        
        // Auto clean logs older than 7 days
        scope.launch {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                database?.systemLogDao()?.deleteLogsOlderThan(sevenDaysAgo)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membersihkan log lama: ${e.message}")
            }
        }
    }

    private fun getUserId(): String {
        return com.example.utils.FirebaseManager.auth?.currentUser?.email ?: "guest"
    }

    private fun log(severity: String, module: String, function: String, message: String, throwable: Throwable? = null) {
        val timestamp = System.currentTimeMillis()
        val logId = UUID.randomUUID().toString()
        val stackTrace = throwable?.let { getStackTraceString(it) }
        
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "SDK ${Build.VERSION.SDK_INT}"
        val appVersion = "1.1.0" // Standard version name
        val userId = getUserId()

        // 1. Logcat
        val logcatMsg = "[$module::$function] $message"
        when (severity) {
            "DEBUG" -> Log.d(TAG, logcatMsg, throwable)
            "INFO" -> Log.i(TAG, logcatMsg, throwable)
            "WARN" -> Log.w(TAG, logcatMsg, throwable)
            "ERROR", "CRITICAL" -> Log.e(TAG, logcatMsg, throwable)
        }

        // 2. Room database (async)
        scope.launch {
            try {
                val entity = SystemLog(
                    logId = logId,
                    timestamp = timestamp,
                    severity = severity,
                    module = module,
                    functionName = function,
                    message = message,
                    deviceInfo = deviceInfo,
                    androidVersion = androidVersion,
                    appVersion = appVersion,
                    stackTrace = stackTrace,
                    userId = userId,
                    sessionId = sessionId
                )
                database?.systemLogDao()?.insertLog(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menyimpan log ke Room: ${e.message}")
            }
        }

        // 3. Flat File with Rotation
        scope.launch {
            try {
                writeToLogFile(timestamp, severity, module, function, message, stackTrace)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menulis ke file log: ${e.message}")
            }
        }
    }

    fun debug(module: String, function: String, message: String) = log("DEBUG", module, function, message)
    fun info(module: String, function: String, message: String) = log("INFO", module, function, message)
    fun warn(module: String, function: String, message: String) = log("WARN", module, function, message)
    fun error(module: String, function: String, message: String, throwable: Throwable? = null) = log("ERROR", module, function, message, throwable)
    fun critical(module: String, function: String, message: String, throwable: Throwable? = null) = log("CRITICAL", module, function, message, throwable)

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    @Synchronized
    private fun writeToLogFile(
        timestamp: Long,
        severity: String,
        module: String,
        function: String,
        message: String,
        stackTrace: String?
    ) {
        val ctx = applicationContext ?: return
        val logFile = File(ctx.filesDir, LOG_FILE_NAME)

        // Check if rotation is needed
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            val oldFile = File(ctx.filesDir, ROTATED_LOG_FILE_NAME)
            if (oldFile.exists()) oldFile.delete()
            logFile.renameTo(oldFile)
        }

        // Write log entry (encrypted lines for maximum production security)
        val rawLine = JSONObject().apply {
            put("timestamp", timestamp)
            put("severity", severity)
            put("module", module)
            put("function", function)
            put("message", message)
            if (stackTrace != null) put("stackTrace", stackTrace)
            put("userId", getUserId())
            put("sessionId", sessionId)
        }.toString()

        val encryptedLine = CryptoUtils.encrypt(rawLine) + "\n"
        FileOutputStream(logFile, true).use { fos ->
            fos.write(encryptedLine.toByteArray(Charsets.UTF_8))
        }
    }

    fun readLogFileDecrypted(): List<JSONObject> {
        val ctx = applicationContext ?: return emptyList()
        val logFile = File(ctx.filesDir, LOG_FILE_NAME)
        if (!logFile.exists()) return emptyList()

        val logs = mutableListOf<JSONObject>()
        logFile.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotEmpty()) {
                    try {
                        val decrypted = CryptoUtils.decrypt(line)
                        logs.add(JSONObject(decrypted))
                    } catch (e: Exception) {
                        // Skip corrupted/invalid encrypted lines
                    }
                }
            }
        }
        return logs
    }

    fun exportLogsToTxt(context: Context): File {
        val exportFile = File(context.cacheDir, "exported_system_logs_${System.currentTimeMillis()}.txt")
        val decryptedLogs = readLogFileDecrypted()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        exportFile.printWriter().use { out ->
            decryptedLogs.forEach { log ->
                val timeStr = sdf.format(Date(log.optLong("timestamp")))
                val severity = log.optString("severity")
                val module = log.optString("module")
                val function = log.optString("function")
                val message = log.optString("message")
                val userId = log.optString("userId")
                val stack = log.optString("stackTrace", "")

                out.println("[$timeStr] [$severity] [$module::$function] (User: $userId) -> $message")
                if (stack.isNotEmpty()) {
                    out.println(stack)
                }
            }
        }
        return exportFile
    }

    fun exportLogsToJson(context: Context): File {
        val exportFile = File(context.cacheDir, "exported_system_logs_${System.currentTimeMillis()}.json")
        val decryptedLogs = readLogFileDecrypted()
        val array = JSONArray()
        decryptedLogs.forEach { array.put(it) }

        exportFile.writeText(array.toString(2))
        return exportFile
    }

    fun exportLogsCompressed(context: Context): File {
        val txtFile = exportLogsToTxt(context)
        val gzipFile = File(context.cacheDir, "exported_system_logs_${System.currentTimeMillis()}.txt.gz")
        
        gzipFile.outputStream().use { fos ->
            GZIPOutputStream(fos).use { gzos ->
                txtFile.inputStream().use { fis ->
                    fis.copyTo(gzos)
                }
            }
        }
        txtFile.delete()
        return gzipFile
    }
}
