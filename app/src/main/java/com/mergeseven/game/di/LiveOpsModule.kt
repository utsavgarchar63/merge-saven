package com.mergeseven.game.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.mergeseven.game.core.analytics.AnalyticsBackend
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.analytics.FirebaseAnalyticsBackend
import com.mergeseven.game.core.analytics.FirebaseAnalyticsTracker
import com.mergeseven.game.core.crash.CrashReporter
import com.mergeseven.game.core.crash.CrashlyticsReporter
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.liveops.RemoteConfigRepository
import com.mergeseven.game.core.liveops.SeasonalEventRecorder
import com.mergeseven.game.core.liveops.SeasonalEventStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveOpsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsBackend(impl: FirebaseAnalyticsBackend): AnalyticsBackend

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: CrashlyticsReporter): CrashReporter

    @Binds
    @Singleton
    abstract fun bindSeasonalEventRecorder(impl: SeasonalEventStore): SeasonalEventRecorder

    @Binds
    @Singleton
    abstract fun bindLevelPackCache(impl: com.mergeseven.game.game.levels.FileLevelPackCache): com.mergeseven.game.game.levels.LevelPackCache

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(
            @ApplicationContext context: Context
        ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics =
            FirebaseCrashlytics.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig =
            FirebaseRemoteConfig.getInstance()

        @Provides
        @Singleton
        fun provideLiveConfig(repo: RemoteConfigRepository): LiveConfig = repo.liveConfig
    }
}
