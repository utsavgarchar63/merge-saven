package com.mergeseven.game.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.GatedFeatureFlags
import com.mergeseven.game.core.flags.LocalFeatureFlags
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeatureFlagsStore

private val Context.featureFlagsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feature_flags"
)

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagsModule {

    @Binds
    @Singleton
    abstract fun bindFeatureFlags(impl: GatedFeatureFlags): FeatureFlags

    companion object {
        @Provides
        @Singleton
        @FeatureFlagsStore
        fun provideFeatureFlagsDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = context.featureFlagsDataStore
    }
}
