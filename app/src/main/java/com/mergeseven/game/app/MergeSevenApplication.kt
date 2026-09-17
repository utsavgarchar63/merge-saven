package com.mergeseven.game.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mergeseven.game.cloud.CloudSyncCoordinator
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.liveops.LevelPackDownloader
import com.mergeseven.game.core.liveops.PushTopicManager
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
class MergeSevenApplication : Application() {

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
    lateinit var pushTopicManager: PushTopicManager

    @Inject
    lateinit var cloudSyncCoordinator: CloudSyncCoordinator

    override fun onCreate() {
        super.onCreate()

        // Off the main thread: this opens the database, and startup must not wait on disk.
        persistenceScope.launch { persistenceSeeder.seedIfNeeded() }

        analyticsTracker.logEvent(AnalyticsEvents.APP_OPEN)

        // AF6: apply last-activated RC immediately, fetch in background (never block startup).
        remoteConfigRepository.bootstrapFromActivatedCache()
        remoteConfigRepository.fetchAsync()
        levelPackDownloader.refreshAsync()
        pushTopicManager.syncTopicsAsync()

        // AF7: silent sign-in + queue drain when enabled; background upload on process stop.
        cloudSyncCoordinator.bootstrap()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    cloudSyncCoordinator.onAppBackgrounded()
                }
            }
        )
    }
}
