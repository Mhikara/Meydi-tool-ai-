package com.example.centralapi.core

import android.content.Context
import androidx.work.*
import com.example.centralapi.database.CentralCacheDatabase
import com.example.centralapi.network.ApiManager
import com.example.centralapi.repository.*
import com.example.centralapi.security.CentralSecurityManager
import com.example.centralapi.worker.CentralSyncWorker
import com.example.core.AppModule
import java.util.concurrent.TimeUnit

class CentralApiModule : AppModule {
    override val id: String = "core.centralapi"

    lateinit var securityManager: CentralSecurityManager
        private set

    lateinit var apiManager: ApiManager
        private set

    lateinit var cacheDatabase: CentralCacheDatabase
        private set

    // Repositories
    lateinit var loginRepository: LoginRepository
    lateinit var registerRepository: RegisterRepository
    lateinit var guestRepository: GuestRepository
    lateinit var profileRepository: ProfileRepository
    lateinit var dashboardRepository: DashboardRepository
    lateinit var updateRepository: UpdateRepository
    lateinit var downloaderRepository: DownloaderRepository
    lateinit var aiAssistantRepository: AiAssistantRepository
    lateinit var premiumRepository: PremiumRepository
    lateinit var syncRepository: SyncRepository
    lateinit var notificationRepository: NotificationRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var statsRepository: StatsRepository
    lateinit var activityRepository: ActivityRepository
    lateinit var fileRepository: FileRepository
    lateinit var searchRepository: SearchRepository
    lateinit var feedbackRepository: FeedbackRepository
    lateinit var ownerRepository: OwnerRepository
    lateinit var adminRepository: AdminRepository

    override fun init(context: Context) {
        // 1. Initialize Security Manager
        securityManager = CentralSecurityManager(context)

        // 2. Initialize Cache database & DAO
        cacheDatabase = CentralCacheDatabase.getDatabase(context)
        val cacheDao = cacheDatabase.cacheDao()

        // 3. Initialize ApiManager
        apiManager = ApiManager(context, securityManager)

        // 4. Initialize all repositories
        loginRepository = LoginRepository(apiManager, cacheDao, securityManager)
        registerRepository = RegisterRepository(apiManager, cacheDao, securityManager)
        guestRepository = GuestRepository(apiManager, cacheDao, securityManager)
        profileRepository = ProfileRepository(apiManager, cacheDao, securityManager)
        dashboardRepository = DashboardRepository(apiManager, cacheDao, securityManager)
        updateRepository = UpdateRepository(apiManager, cacheDao, securityManager)
        downloaderRepository = DownloaderRepository(apiManager, cacheDao, securityManager)
        aiAssistantRepository = AiAssistantRepository(apiManager, cacheDao, securityManager)
        premiumRepository = PremiumRepository(apiManager, cacheDao, securityManager)
        syncRepository = SyncRepository(apiManager, cacheDao, securityManager)
        notificationRepository = NotificationRepository(apiManager, cacheDao, securityManager)
        settingsRepository = SettingsRepository(apiManager, cacheDao, securityManager)
        statsRepository = StatsRepository(apiManager, cacheDao, securityManager)
        activityRepository = ActivityRepository(apiManager, cacheDao, securityManager)
        fileRepository = FileRepository(apiManager, cacheDao, securityManager)
        searchRepository = SearchRepository(apiManager, cacheDao, securityManager)
        feedbackRepository = FeedbackRepository(apiManager, cacheDao, securityManager)
        ownerRepository = OwnerRepository(apiManager, cacheDao, securityManager)
        adminRepository = AdminRepository(apiManager, cacheDao, securityManager)

        // 5. Schedule Background WorkManager sync task automatically (runs periodically every 15 minutes)
        val syncRequest = PeriodicWorkRequestBuilder<CentralSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CentralCloudSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
