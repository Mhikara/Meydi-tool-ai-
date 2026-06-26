package com.example.centralapi.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.centralapi.core.CentralApiModule
import com.example.core.Kernel

class CentralSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("CentralSyncWorker", "Starting background data synchronization worker...")

        // Retrieve CentralApiModule from Meydi's modular Kernel
        val module = Kernel.get<CentralApiModule>("core.centralapi")
        if (module == null) {
            Log.e("CentralSyncWorker", "Failed to retrieve CentralApiModule. Kernel is not booted.")
            return Result.failure()
        }

        return try {
            // Synchronize local cached activities/logs with server
            val syncResult = module.syncRepository.syncOfflineData()
            if (syncResult.isSuccess) {
                Log.i("CentralSyncWorker", "Background sync completed successfully!")
                Result.success()
            } else {
                Log.w("CentralSyncWorker", "Background sync failed. Will retry.", syncResult.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("CentralSyncWorker", "Unexpected error during background synchronization", e)
            Result.failure()
        }
    }
}
