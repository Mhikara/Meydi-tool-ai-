package com.example.recovery

import android.content.Context
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object RecoveryManager {
    private const val BACKUP_DIR = "database_backups"
    private const val DB_NAME = "meydiai_database"
    private var applicationContext: Context? = null
    private var database: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        val appCtx = context.applicationContext
        applicationContext = appCtx
        database = AppDatabase.getDatabase(appCtx)

        // Verify database integrity on launch
        scope.launch {
            try {
                verifyAndRecoverDatabase()
            } catch (e: Exception) {
                AppLogger.critical("RecoveryManager", "init", "Fatal error during database integrity verification: ${e.message}", e)
            }
        }
    }

    private suspend fun verifyAndRecoverDatabase() {
        val ctx = applicationContext ?: return
        val dbFile = ctx.getDatabasePath(DB_NAME)
        
        if (dbFile.exists()) {
            val isHealthy = checkDatabaseIntegrity(ctx)
            if (!isHealthy) {
                AppLogger.critical("RecoveryManager", "verifyAndRecoverDatabase", "Kerusakan database (Corruption) terdeteksi! Memulai pemulihan otomatis...")
                val success = rollbackToLatestBackup()
                if (success) {
                    AppLogger.info("RecoveryManager", "verifyAndRecoverDatabase", "Database berhasil dipulihkan dari cadangan valid terakhir.")
                } else {
                    AppLogger.critical("RecoveryManager", "verifyAndRecoverDatabase", "Gagal memulihkan database. Cadangan tidak ditemukan atau rusak.")
                }
            } else {
                AppLogger.info("RecoveryManager", "verifyAndRecoverDatabase", "Database dalam kondisi sehat (PRAGMA integrity_check = OK).")
            }
        }
    }

    private fun checkDatabaseIntegrity(context: Context): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Execute standard SQLite integrity check
            val cursor = db.openHelper.writableDatabase.query("PRAGMA integrity_check")
            var isOk = false
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                isOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()
            isOk
        } catch (e: Exception) {
            AppLogger.error("RecoveryManager", "checkDatabaseIntegrity", "Gagal melakukan pemeriksaan integritas database: ${e.message}", e)
            false
        }
    }

    suspend fun createBackup(description: String): Boolean {
        val ctx = applicationContext ?: return false
        val dbFile = ctx.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return false

        return try {
            val backupDirectory = File(ctx.filesDir, BACKUP_DIR)
            if (!backupDirectory.exists()) backupDirectory.mkdirs()

            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDirectory, "${DB_NAME}_backup_$timestamp.db")

            // Copy Database File
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Calculate MD5 Checksum
            val checksum = calculateChecksum(backupFile)

            val record = BackupRecord(
                timestamp = timestamp,
                filePath = backupFile.absolutePath,
                checksum = checksum,
                description = description,
                isValid = true
            )

            database?.backupRecordDao()?.insertRecord(record)
            AppLogger.info("RecoveryManager", "createBackup", "Cadangan database berhasil dibuat: ${backupFile.name} (MD5: $checksum)")
            true
        } catch (e: Exception) {
            AppLogger.error("RecoveryManager", "createBackup", "Gagal membuat cadangan database: ${e.message}", e)
            false
        }
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun rollbackToLatestBackup(): Boolean {
        val ctx = applicationContext ?: return false
        val dao = database?.backupRecordDao() ?: return false
        val latestBackup = dao.getLatestValidBackup() ?: return false

        val backupFile = File(latestBackup.filePath)
        if (!backupFile.exists()) {
            dao.deleteBackupRecord(latestBackup.id)
            return false
        }

        // Verify checksum integrity
        val currentChecksum = calculateChecksum(backupFile)
        if (currentChecksum != latestBackup.checksum) {
            AppLogger.critical("RecoveryManager", "rollbackToLatestBackup", "Integritas berkas cadangan rusak (MD5 Mismatch). Membatalkan rollback.")
            dao.deleteBackupRecord(latestBackup.id)
            return false
        }

        return try {
            // Close database first
            database?.close()

            val dbFile = ctx.getDatabasePath(DB_NAME)
            val dbWalFile = ctx.getDatabasePath("$DB_NAME-wal")
            val dbShmFile = ctx.getDatabasePath("$DB_NAME-shm")

            // Delete existing database files to clear lock
            if (dbFile.exists()) dbFile.delete()
            if (dbWalFile.exists()) dbWalFile.delete()
            if (dbShmFile.exists()) dbShmFile.delete()

            // Copy Backup back
            backupFile.inputStream().use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Re-open database
            database = AppDatabase.getDatabase(ctx)
            AppLogger.info("RecoveryManager", "rollbackToLatestBackup", "Rollback database berhasil diselesaikan ke cadangan tanggal ${latestBackup.timestamp}")
            true
        } catch (e: Exception) {
            AppLogger.error("RecoveryManager", "rollbackToLatestBackup", "Gagal memulihkan database selama proses rollback: ${e.message}", e)
            false
        }
    }
}
