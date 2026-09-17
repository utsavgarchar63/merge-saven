package com.mergeseven.game.cloud

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.testing.TestPersistence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncCoordinatorTest {

    @Test
    fun guestDoesNotUpload() = runTest {
        val cloud = InMemoryCloudSaveRepository()
        val auth = FakePlayGamesAuth(initiallySignedIn = null)
        val queue = InMemorySyncQueue()
        FakeCloudStack.coordinator(
            auth = auth,
            cloudSave = cloud,
            syncQueue = queue
        ).requestUpload(UploadReason.LEVEL_COMPLETE)
        assertEquals(0, cloud.writeCount)
        assertTrue(queue.state.pendingReasons.isEmpty())
    }

    @Test
    fun ambiguousEmitsConflictWithoutSilentApply() = runTest {
        val equalLocal = UserProfile(coins = 200, totalStars = 5, totalMerges = 10)
        val equalCloud = CloudSnapshot(
            deviceId = "c",
            updatedAtEpochMs = 2L,
            profile = equalLocal.toPayload().copy(equippedTileThemeId = "marble"),
            levels = emptyList()
        )
        val cloud = InMemoryCloudSaveRepository().also { it.stored = equalCloud }
        val events = mutableListOf<CloudUiEvent>()
        val coordinator = FakeCloudStack.coordinator(
            auth = FakePlayGamesAuth(CloudAccount("p1", "T")),
            cloudSave = cloud,
            profileStore = InMemoryUserProfileStore(equalLocal)
        )
        val job = launch(UnconfinedTestDispatcher()) {
            coordinator.events.collect { events += it }
        }
        coordinator.requestUpload(UploadReason.MANUAL)
        job.cancel()
        assertTrue(events.any { it is CloudUiEvent.Conflict })
        assertEquals(0, cloud.writeCount)
    }

    @Test
    fun corruptSnapshotLeavesLocalUntouched() {
        assertNull(decodeCloudSnapshotOrNull("{not json}"))
        assertNull(
            decodeCloudSnapshotOrNull(
                """{"schemaVersion":99,"deviceId":"x","updatedAtEpochMs":1,"profile":{"coins":1,"totalStars":0}}"""
            )
        )
    }

    @Test
    fun queueBackoffGrowsThenCaps() {
        val queue = InMemorySyncQueue()
        assertEquals(60_000L, queue.backoffMs(0))
        assertEquals(120_000L, queue.backoffMs(1))
        assertEquals(3_600_000L, queue.backoffMs(10))
    }

    @Test
    fun mergeSafeNeverDropsCoins() {
        val preferred = CloudSnapshot(
            deviceId = "a",
            updatedAtEpochMs = 1,
            profile = UserProfile(coins = 50, totalStars = 3).toPayload()
        )
        val other = CloudSnapshot(
            deviceId = "b",
            updatedAtEpochMs = 2,
            profile = UserProfile(coins = 400, totalStars = 1).toPayload(),
            unlocks = listOf(UnlockPayload("cosmetic_x", "cosmetic", 1, 1L))
        )
        val applier = CloudSnapshotApplier(
            userDataRepository = TestPersistence.userDataRepository(),
            levelProgressStore = com.mergeseven.game.data.local.store.InMemoryLevelProgressStore(),
            levelProgressDao = object : com.mergeseven.game.data.local.dao.LevelProgressDao {
                override fun observeAll() =
                    kotlinx.coroutines.flow.flowOf(emptyList<com.mergeseven.game.data.local.entity.LevelProgressEntity>())
                override suspend fun getAll() =
                    emptyList<com.mergeseven.game.data.local.entity.LevelProgressEntity>()
                override suspend fun upsert(entity: com.mergeseven.game.data.local.entity.LevelProgressEntity) = Unit
                override suspend fun totalStars() = 0
                override suspend fun highestCompletedLevel() = 0
                override suspend fun clear() = Unit
            },
            unlockDao = com.mergeseven.game.testing.InMemoryUnlockDao()
        )
        val merged = applier.mergeSafe(preferred, other)
        assertEquals(400, merged.profile.coins)
        assertEquals(3, merged.profile.totalStars)
        assertEquals(1, merged.unlocks.size)
    }

    @Test
    fun progressCompareDetectsDominance() {
        val low = ProgressTuple(1, 0, 0, 10)
        val high = ProgressTuple(2, 0, 0, 0)
        assertEquals(Dominance.CLOUD, ProgressCompare.compare(low, high))
        assertEquals(Dominance.LOCAL, ProgressCompare.compare(high, low))
        assertEquals(Dominance.AMBIGUOUS, ProgressCompare.compare(low, low))
    }

    @Test
    fun af7OffSkipsUploadEvenWhenSignedIn() = runTest {
        val cloud = InMemoryCloudSaveRepository()
        FakeCloudStack.coordinator(
            featureFlags = InMemoryFeatureFlags(isDebug = true), // AF7 off
            auth = FakePlayGamesAuth(CloudAccount("p1", "T")),
            cloudSave = cloud
        ).requestUpload(UploadReason.ECONOMY)
        assertEquals(0, cloud.writeCount)
    }
}
