package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProjectDraft::class,
        VideoJob::class,
        com.example.logging.SystemLog::class,
        com.example.activity.ActivityHistory::class,
        com.example.cache.CachedData::class,
        com.example.recovery.BackupRecord::class,
        com.example.sync.SyncQueueItem::class,
        com.example.update.model.ChangelogEntity::class,
        com.example.update.model.AppConfigEntity::class,
        com.example.downloader.data.DownloadHistoryEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
    abstract fun videoJobDao(): VideoJobDao
    abstract fun systemLogDao(): com.example.logging.SystemLogDao
    abstract fun activityHistoryDao(): com.example.activity.ActivityHistoryDao
    abstract fun cachedDataDao(): com.example.cache.CachedDataDao
    abstract fun backupRecordDao(): com.example.recovery.BackupRecordDao
    abstract fun syncQueueDao(): com.example.sync.SyncQueueDao
    abstract fun updateDao(): com.example.update.db.UpdateDao
    abstract fun downloadHistoryDao(): com.example.downloader.data.DownloadHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meydiai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
