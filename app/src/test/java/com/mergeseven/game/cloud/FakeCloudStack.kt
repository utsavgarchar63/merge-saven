package com.mergeseven.game.cloud

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.local.dao.LevelProgressDao
import com.mergeseven.game.data.local.entity.LevelProgressEntity
import com.mergeseven.game.data.local.store.InMemoryLevelProgressStore
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.testing.InMemoryUnlockDao
import com.mergeseven.game.testing.TestPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
object FakeCloudStack {

    fun coordinator(
        featureFlags: InMemoryFeatureFlags = InMemoryFeatureFlags(
            isDebug = true,
            initial = mapOf(Feature.AF7 to true)
        ),
        auth: FakePlayGamesAuth = FakePlayGamesAuth(
            CloudAccount("player_1", "Tester")
        ),
        cloudSave: InMemoryCloudSaveRepository = InMemoryCloudSaveRepository(),
        syncQueue: InMemorySyncQueue = InMemorySyncQueue(),
        profileStore: InMemoryUserProfileStore = InMemoryUserProfileStore(UserProfile()),
        scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    ): CloudSyncCoordinator {
        val levelStore = InMemoryLevelProgressStore()
        val unlockDao = InMemoryUnlockDao()
        val userRepo = TestPersistence.userDataRepository(
            store = profileStore,
            scope = scope,
            featureFlags = featureFlags
        )
        val builder = CloudSnapshotBuilder(
            userProfileStore = profileStore,
            levelProgressStore = levelStore,
            unlockDao = unlockDao,
            deviceIdStore = FixedDeviceIdStore("test-device")
        )
        val applier = CloudSnapshotApplier(
            userDataRepository = userRepo,
            levelProgressStore = levelStore,
            levelProgressDao = object : LevelProgressDao {
                override fun observeAll(): Flow<List<LevelProgressEntity>> = flowOf(emptyList())
                override suspend fun getAll(): List<LevelProgressEntity> = emptyList()
                override suspend fun upsert(entity: LevelProgressEntity) = Unit
                override suspend fun totalStars(): Int = 0
                override suspend fun highestCompletedLevel(): Int = 0
                override suspend fun clear() = Unit
            },
            unlockDao = unlockDao
        )
        return CloudSyncCoordinator(
            featureFlags = featureFlags,
            auth = auth,
            cloudSave = cloudSave,
            builder = builder,
            applier = applier,
            syncQueue = syncQueue,
            economyNotifier = CloudEconomyNotifier(),
            scope = scope
        )
    }
}
