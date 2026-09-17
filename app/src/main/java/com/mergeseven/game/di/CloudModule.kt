package com.mergeseven.game.di

import com.mergeseven.game.cloud.CloudSaveRepository
import com.mergeseven.game.cloud.CloudSyncCoordinator
import com.mergeseven.game.cloud.CloudUploadTrigger
import com.mergeseven.game.cloud.DataStoreDeviceIdStore
import com.mergeseven.game.cloud.DataStoreSyncQueue
import com.mergeseven.game.cloud.DeviceIdStore
import com.mergeseven.game.cloud.PlayGamesAuth
import com.mergeseven.game.cloud.PlayGamesAuthImpl
import com.mergeseven.game.cloud.PlayGamesCloudSaveRepository
import com.mergeseven.game.cloud.SyncQueue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudModule {

    @Binds
    @Singleton
    abstract fun bindPlayGamesAuth(impl: PlayGamesAuthImpl): PlayGamesAuth

    @Binds
    @Singleton
    abstract fun bindCloudSaveRepository(impl: PlayGamesCloudSaveRepository): CloudSaveRepository

    @Binds
    @Singleton
    abstract fun bindSyncQueue(impl: DataStoreSyncQueue): SyncQueue

    @Binds
    @Singleton
    abstract fun bindDeviceIdStore(impl: DataStoreDeviceIdStore): DeviceIdStore

    @Binds
    @Singleton
    abstract fun bindCloudUploadTrigger(impl: CloudSyncCoordinator): CloudUploadTrigger
}
