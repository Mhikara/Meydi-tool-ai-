package com.example.sync

import android.content.Context
import androidx.work.*
import com.example.logging.AppLogger
import java.util.concurrent.TimeUnit

class BackgroundSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.info("BackgroundSyncWorker", "doWork", "Pekerjaan sinkronisasi latar belakang dipicu.")
        return try {
            SyncManager.processSyncQueue()
            Result.success()
        } catch (e: Exception) {
            AppLogger.error("BackgroundSyncWorker", "doWork", "Sinkronisasi latar belakang gagal: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "meydiai_periodic_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                15, TimeUnit.MINUTES // Minimum interval allowed by Android WorkManager
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid resetting the 15-minute timer
                periodicSyncRequest
            )
            AppLogger.info("BackgroundSyncWorker", "schedulePeriodicSync", "Sinkronisasi latar belakang berkala berhasil dijadwalkan (setiap 15 menit).")
        }
    }
}
