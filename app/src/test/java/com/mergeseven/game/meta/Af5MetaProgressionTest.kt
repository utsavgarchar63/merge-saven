package com.mergeseven.game.meta

import com.mergeseven.game.cloud.CloudEconomyNotifier
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.testing.InMemoryUnlockDao
import com.mergeseven.game.testing.TestPersistence
import com.mergeseven.game.ui.theme.ClassicTileTheme
import com.mergeseven.game.ui.theme.GameColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Af5MetaProgressionTest {

    @Test
    fun xpCurveLevelsUpAtHundredTimesLevel() {
        val (xp, level, gained) = XpCurve.apply(currentXp = 90, currentLevel = 1, amount = 20)
        assertEquals(10, xp)
        assertEquals(2, level)
        assertEquals(1, gained)
    }

    @Test
    fun xpCurveIgnoresNonPositive() {
        val (xp, level, gained) = XpCurve.apply(50, 3, 0)
        assertEquals(50, xp)
        assertEquals(3, level)
        assertEquals(0, gained)
    }

    @Test
    fun addXpPersistsAcrossRepositoryRestart() {
        val store = InMemoryUserProfileStore()
        val first = TestPersistence.userDataRepository(store)
        first.addXp(100)
        assertEquals(2, first.userProfile.value.playerLevel)
        assertEquals(0, first.userProfile.value.xp)

        val second = TestPersistence.userDataRepository(store)
        assertEquals(2, second.userProfile.value.playerLevel)
        assertEquals(0, second.userProfile.value.xp)
    }

    @Test
    fun classicTileThemeMatchesGameColorsForCommonValues() {
        for (value in listOf(2, 4, 8, 16, 32, 64)) {
            assertEquals(GameColors.tileColor(value), ClassicTileTheme.tileColor(value))
        }
    }

    @Test
    fun achievementCompletesOnceAndSetsToast() = runTest {
        val dao = InMemoryUnlockDao()
        val tracker = AchievementTracker(dao, TestPersistence.dispatchers())
        tracker.load()

        val first = tracker.reportMetric(AchievementMetric.MERGES, 1)
        assertEquals(1, first.size)
        assertEquals("first_merge", first.first().id)
        assertEquals("First Merge", tracker.newlyCompleted.value?.title)

        val second = tracker.reportMetric(AchievementMetric.MERGES, 1)
        assertTrue(second.none { it.id == "first_merge" })
        tracker.consumeToast()
        assertEquals(null, tracker.newlyCompleted.value)
    }

    @Test
    fun cosmeticBuyRejectsOverdraftThenGrantsAndEquipPersists() = runTest {
        val store = InMemoryUserProfileStore()
        val repo = TestPersistence.userDataRepository(store)
        // Drain wallet below marble tile cost (150)
        while (repo.userProfile.value.coins >= 150) {
            assertTrue(repo.trySpendCoins(50))
        }
        val dao = InMemoryUnlockDao()
        val unlocks = UnlockService(
            dao,
            TestPersistence.dispatchers(),
            repo,
            InMemoryFeatureFlags(isDebug = true),
            CloudEconomyNotifier()
        )
        unlocks.loadAndSeed()

        assertFalse(unlocks.tryBuyCosmetic(CosmeticCatalog.TILE_MARBLE))
        assertFalse(unlocks.owns(CosmeticCatalog.TILE_MARBLE))

        repo.addCoins(200)
        assertTrue(unlocks.tryBuyCosmetic(CosmeticCatalog.TILE_MARBLE))
        assertTrue(unlocks.owns(CosmeticCatalog.TILE_MARBLE))

        repo.equipTileTheme(CosmeticCatalog.TILE_MARBLE)
        val restarted = TestPersistence.userDataRepository(store)
        assertEquals(CosmeticCatalog.TILE_MARBLE, restarted.userProfile.value.equippedTileThemeId)
    }

    @Test
    fun milestoneClaimOnceSecondClaimNoOp() = runTest {
        val store = InMemoryUserProfileStore()
        val repo = TestPersistence.userDataRepository(store)
        repo.recordLifetimeStats(mergesDelta = 100)
        val dao = InMemoryUnlockDao()
        val unlocks = UnlockService(
            dao,
            TestPersistence.dispatchers(),
            repo,
            InMemoryFeatureFlags(isDebug = true),
            CloudEconomyNotifier()
        )
        unlocks.loadAndSeed()

        val def = MilestoneCatalog.all.first { it.id == "merges_100" }
        val coinsBefore = repo.userProfile.value.coins
        assertTrue(unlocks.claimMilestone(def))
        assertEquals(coinsBefore + def.coinsReward, repo.userProfile.value.coins)
        assertTrue(unlocks.isMilestoneClaimed(def.id))
        assertFalse(unlocks.claimMilestone(def))
    }
}
