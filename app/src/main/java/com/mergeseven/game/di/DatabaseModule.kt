package com.mergeseven.game.di

import android.content.Context
import androidx.room.Room
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.GameDatabase
import com.mergeseven.game.data.local.GameDatabaseMigrations
import com.mergeseven.game.data.local.dao.ActiveGameDao
import com.mergeseven.game.data.local.dao.LevelProgressDao
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.dao.UserProfileDao
import com.mergeseven.game.data.local.store.LevelProgressStore
import com.mergeseven.game.data.local.store.ModeRecordsStore
import com.mergeseven.game.data.local.store.RoomLevelProgressStore
import com.mergeseven.game.data.local.store.RoomModeRecordsStore
import com.mergeseven.game.data.local.store.RoomUserProfileStore
import com.mergeseven.game.data.local.store.UserProfileStore
import com.mergeseven.game.game.repository.GameRepository
import com.mergeseven.game.game.repository.RoomGameRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileStore(impl: RoomUserProfileStore): UserProfileStore

    @Binds
    @Singleton
    abstract fun bindLevelProgressStore(impl: RoomLevelProgressStore): LevelProgressStore

    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: RoomGameRepository): GameRepository

    @Binds
    @Singleton
    abstract fun bindModeRecordsStore(impl: RoomModeRecordsStore): ModeRecordsStore

    @Binds
    @Singleton
    abstract fun bindBoosterInventoryStore(
        impl: com.mergeseven.game.data.local.store.RoomBoosterInventoryStore
    ): com.mergeseven.game.data.local.store.BoosterInventoryStore

    companion object {

        @Provides
        @Singleton
        fun provideGameDatabase(
            @ApplicationContext context: Context
        ): GameDatabase = Room.databaseBuilder(
            context,
            GameDatabase::class.java,
            GameDatabase.NAME
        ).addMigrations(*GameDatabaseMigrations.ALL).build()

        @Provides
        fun provideActiveGameDao(database: GameDatabase): ActiveGameDao = database.activeGameDao()

        @Provides
        fun provideLevelProgressDao(database: GameDatabase): LevelProgressDao =
            database.levelProgressDao()

        @Provides
        fun provideUserProfileDao(database: GameDatabase): UserProfileDao = database.userProfileDao()

        @Provides
        fun provideUnlockDao(database: GameDatabase): UnlockDao = database.unlockDao()

        @OptIn(ExperimentalCoroutinesApi::class)
        @Provides
        @Singleton
        @PersistenceScope
        fun providePersistenceScope(dispatchers: DispatcherProvider): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatchers.io.limitedParallelism(1))
    }
}
