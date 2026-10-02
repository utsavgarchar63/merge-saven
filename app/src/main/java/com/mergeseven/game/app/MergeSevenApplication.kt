package com.mergeseven.game.app

import android.app.Application
import androidx.work.Configuration
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.liveops.LevelPackDownloader
import com.mergeseven.game.core.liveops.LocalReminderManager
import com.mergeseven.game.core.liveops.RemoteConfigRepository
import com.mergeseven.game.data.local.PersistenceSeeder
import com.mergeseven.game.di.PersistenceScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Merge Seven.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class MergeSevenApplication : Application(), Configuration.Provider {

    // WorkManager can initialize safely from the background reminder sync, including cold starts.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()

    @Inject
    lateinit var persistenceSeeder: PersistenceSeeder

    @PersistenceScope
    @Inject
    lateinit var persistenceScope: CoroutineScope

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    @Inject
    lateinit var levelPackDownloader: LevelPackDownloader

    @Inject
    lateinit var reminderManager: LocalReminderManager


    override fun onCreate() {
        super.onCreate()


        // Off the main thread: this opens the database, and startup must not wait on disk.
        persistenceScope.launch { persistenceSeeder.seedIfNeeded() }

        analyticsTracker.logEvent(AnalyticsEvents.APP_OPEN)

        // AF6: apply last-activated RC immediately, fetch in background (never block startup).
        remoteConfigRepository.bootstrapFromActivatedCache()
        remoteConfigRepository.fetchAsync()
        levelPackDownloader.refreshAsync()
        reminderManager.syncAsync()

    }
}
