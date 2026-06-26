package com.example.activity

import android.content.Context
import android.provider.Settings
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import com.example.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.util.*

object ActivityLogger {
    private const val TAG = "ActivityLogger"
    private var applicationContext: Context? = null
    private var database: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        val appCtx = context.applicationContext
        applicationContext = appCtx
        database = AppDatabase.getDatabase(appCtx)
    }

    private fun getDeviceId(): String {
        val ctx = applicationContext ?: return "unknown_device"
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_android_id"
    }

    private fun getIpAddress(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
            "127.0.0.1"
        } catch (ex: Exception) {
            "0.0.0.0"
        }
    }

    fun logActivity(
        name: String,
        description: String,
        isSuccess: Boolean = true,
        additionalDetails: Map<String, Any> = emptyMap()
    ) {
        val timestamp = System.currentTimeMillis()
        val activityId = UUID.randomUUID().toString()
        val userId = FirebaseManager.auth?.currentUser?.email ?: "guest"
        val deviceId = getDeviceId()
        val ipAddress = getIpAddress()
        val detailsStr = additionalDetails.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val activity = ActivityHistory(
            activityId = activityId,
            name = name,
            description = description,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            ipAddress = ipAddress,
            isSuccess = isSuccess,
            additionalDetails = detailsStr
        )

        AppLogger.info("ActivityLogger", "logActivity", "Aktivitas Terdeteksi: $name - $description (Success: $isSuccess)")

        // 1. Save to Room database (Offline Cache)
        scope.launch {
            try {
                database?.activityHistoryDao()?.insertActivity(activity)
            } catch (e: Exception) {
                AppLogger.error("ActivityLogger", "logActivity", "Gagal menyimpan aktivitas ke SQLite: ${e.message}", e)
            }
        }

        // 2. Save/Sync to Firebase Cloud Firestore (Online Sync)
        scope.launch {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null && userId != "guest") {
                    val firestoreData = mapOf(
                        "activityId" to activityId,
                        "name" to name,
                        "description" to description,
                        "timestamp" to timestamp,
                        "userId" to userId,
                        "deviceId" to deviceId,
                        "ipAddress" to ipAddress,
                        "isSuccess" to isSuccess,
                        "additionalDetails" to additionalDetails
                    )
                    firestore.collection("users")
                        .document(userId)
                        .collection("activities")
                        .document(activityId)
                        .set(firestoreData)
                        .addOnSuccessListener {
                            AppLogger.debug("ActivityLogger", "logActivity", "Aktivitas berhasil disinkronkan ke Firestore.")
                        }
                        .addOnFailureListener { e ->
                            AppLogger.warn("ActivityLogger", "logActivity", "Gagal sync aktivitas ke Firestore (offline queue diaktifkan): ${e.message}")
                            // We can queue it
                            queueSyncAction("activities", activityId, firestoreData, "CREATE")
                        }
                }
            } catch (e: Exception) {
                AppLogger.warn("ActivityLogger", "logActivity", "Firebase offline atau tidak tersedia. Aktivitas di-cache lokal.")
            }
        }
    }

    private fun queueSyncAction(collection: String, docId: String, data: Map<String, Any>, operation: String) {
        scope.launch {
            try {
                val json = org.json.JSONObject(data).toString()
                val queueItem = com.example.sync.SyncQueueItem(
                    collectionName = collection,
                    docId = docId,
                    dataJson = json,
                    operationType = operation,
                    timestamp = System.currentTimeMillis()
                )
                database?.syncQueueDao()?.insertQueueItem(queueItem)
            } catch (e: Exception) {
                AppLogger.error("ActivityLogger", "queueSyncAction", "Gagal mengantrekan aksi sinkronisasi offline: ${e.message}", e)
            }
        }
    }
}
