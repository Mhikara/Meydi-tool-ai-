package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import com.example.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object SyncManager {
    private var applicationContext: Context? = null
    private var database: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val error: String) : SyncStatus()
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        applicationContext = appCtx
        database = AppDatabase.getDatabase(appCtx)

        registerNetworkCallback(appCtx)
        autoSync()
    }

    private fun registerNetworkCallback(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
        cm.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                AppLogger.info("SyncManager", "onAvailable", "Koneksi internet pulih. Memulai pengosongan antrean sinkronisasi (Sync Queue)...")
                autoSync()
            }
        })
    }

    fun autoSync() {
        scope.launch {
            try {
                processSyncQueue()
            } catch (e: Exception) {
                AppLogger.error("SyncManager", "autoSync", "Gagal melakukan sinkronisasi otomatis: ${e.message}")
            }
        }
    }

    suspend fun processSyncQueue() {
        val dao = database?.syncQueueDao() ?: return
        val items = dao.getAllQueueItems()
        if (items.isEmpty()) {
            _syncStatus.value = SyncStatus.Idle
            return
        }

        _syncStatus.value = SyncStatus.Syncing
        AppLogger.info("SyncManager", "processSyncQueue", "Memproses ${items.size} aksi sinkronisasi yang tertunda...")

        var successCount = 0
        items.forEachIndexed { index, item ->
            val success = pushToFirestore(item)
            if (success) {
                dao.deleteQueueItem(item.id)
                successCount++
            } else {
                dao.incrementRetryCount(item.id)
            }
            _uploadProgress.value = (index + 1).toFloat() / items.size.toFloat()
        }

        if (successCount == items.size) {
            _syncStatus.value = SyncStatus.Success
            AppLogger.info("SyncManager", "processSyncQueue", "Seluruh antrean sinkronisasi ($successCount entri) berhasil dikirim ke server.")
        } else {
            _syncStatus.value = SyncStatus.Error("Sebagian data gagal disinkronkan. Sisa antrean: ${items.size - successCount}")
            AppLogger.warn("SyncManager", "processSyncQueue", "Gagal menyelesaikan sinkronisasi penuh. Sisa antrean: ${items.size - successCount}")
        }
    }

    private suspend fun pushToFirestore(item: SyncQueueItem): Boolean {
        val firestore = FirebaseManager.firestore ?: return false
        val currentUser = FirebaseManager.auth?.currentUser?.email ?: return false

        return try {
            val jsonObject = JSONObject(item.dataJson)
            val dataMap = mutableMapOf<String, Any>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                dataMap[key] = jsonObject.get(key)
            }

            // Conflict Resolution / Incremental Sync
            // We check if the document already exists in Firestore and compare timestamps
            val docRef = firestore.collection("users")
                .document(currentUser)
                .collection(item.collectionName)
                .document(item.docId)

            when (item.operationType) {
                "CREATE", "UPDATE" -> {
                    var shouldWrite = true
                    try {
                        val snapshot = com.google.android.gms.tasks.Tasks.await(docRef.get())
                        if (snapshot.exists()) {
                            val serverTimestamp = snapshot.getLong("timestamp") ?: 0L
                            val localTimestamp = (dataMap["timestamp"] as? Long) ?: item.timestamp
                            if (serverTimestamp > localTimestamp) {
                                // Server wins, merge server changes to local or skip writing local to server
                                AppLogger.info("SyncManager", "pushToFirestore", "Konflik terdeteksi untuk dokumen ${item.docId}. Data server lebih baru. Menggunakan strategi Server-Wins.")
                                shouldWrite = false
                            }
                        }
                    } catch (e: Exception) {
                        // Document doesn't exist, proceed to write
                    }

                    if (shouldWrite) {
                        com.google.android.gms.tasks.Tasks.await(docRef.set(dataMap))
                    }
                }
                "DELETE" -> {
                    com.google.android.gms.tasks.Tasks.await(docRef.delete())
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.error("SyncManager", "pushToFirestore", "Gagal menyinkronkan dokumen ${item.docId}: ${e.message}")
            false
        }
    }
}
