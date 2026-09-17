package com.mergeseven.game.ui.daily

import com.mergeseven.game.core.analytics.NoOpAnalyticsTracker
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.testing.TestPersistence
import com.mergeseven.game.testing.fakeContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DailyViewModelTest {

    private lateinit var userDataRepository: UserDataRepository
    private lateinit var audioManager: AudioManager
    private lateinit var viewModel: DailyViewModel

    @Before
    fun setup() {
        userDataRepository = TestPersistence.userDataRepository()
        audioManager = AudioManager(fakeContext())
        viewModel = DailyViewModel(
            userDataRepository = userDataRepository,
            audioManager = audioManager,
            featureFlags = object : FeatureFlags {
                override fun isEnabled(feature: Feature) = false
                override fun observe(feature: Feature) = flowOf(false)
                override suspend fun setEnabled(feature: Feature, enabled: Boolean) = Unit
                override fun snapshot() = Feature.entries.associateWith { false }
            },
            boosterInventory = object : BoosterInventoryStore {
                private val _owned =
                    MutableStateFlow(BoosterCatalog.specs.mapValues { it.value.startingOwned })
                override val owned: StateFlow<Map<BoosterType, Int>> = _owned.asStateFlow()
                override suspend fun loadAndSeed() = Unit
                override fun count(type: BoosterType) = _owned.value[type] ?: 0
                override suspend fun grant(type: BoosterType, amount: Int) {
                    _owned.value = _owned.value + (type to count(type) + amount)
                }
                override suspend fun tryConsume(type: BoosterType) = false
            },
            analyticsTracker = NoOpAnalyticsTracker(),
            adService = com.mergeseven.game.ads.FakeAdService(),
            adPreloader = com.mergeseven.game.ads.AdPreloader(com.mergeseven.game.ads.FakeAdService())
        )
    }

    @Test
    fun `test initial daily rewards list`() {
        val rewards = viewModel.getDailyRewards()
        assertEquals(7, rewards.size)
        assertEquals(1, rewards[0].day)
        assertTrue(rewards[0].isAvailable)
        assertFalse(rewards[0].isClaimed)
        assertFalse(rewards[1].isAvailable)
    }

    @Test
    fun `test claim daily reward updates coins and stars`() {
        val initialCoins = userDataRepository.userProfile.value.coins
        val rewards = viewModel.getDailyRewards()
        val day1Item = rewards[0]

        viewModel.claimReward(day1Item)

        val updatedProfile = userDataRepository.userProfile.value
        assertEquals(initialCoins + 50, updatedProfile.coins)
        assertTrue(updatedProfile.claimedDays.contains(1))
    }

    @Test
    fun `test daily quest progress update and claim`() {
        val initialCoins = userDataRepository.userProfile.value.coins

        userDataRepository.updateQuestProgress("quest_merge", 10)

        val completedQuest = userDataRepository.userProfile.value.dailyQuests.first { it.id == "quest_merge" }
        assertTrue(completedQuest.isCompleted)
        assertFalse(completedQuest.isClaimed)

        viewModel.claimQuest("quest_merge")

        val claimedQuest = userDataRepository.userProfile.value.dailyQuests.first { it.id == "quest_merge" }
        assertTrue(claimedQuest.isClaimed)
        assertEquals(initialCoins + 100, userDataRepository.userProfile.value.coins)
    }

    @Test
    fun `test consecutive login keeps streak and missed login resets streak`() {
        userDataRepository.checkDailyLogin("2026-08-01")
        assertEquals(1, userDataRepository.userProfile.value.currentStreak)

        // Day 2 (consecutive)
        userDataRepository.checkDailyLogin("2026-08-02")
        assertEquals(1, userDataRepository.userProfile.value.currentStreak)

        // Day 4 (missed day 3)
        userDataRepository.checkDailyLogin("2026-08-04")
        assertEquals(1, userDataRepository.userProfile.value.currentStreak)
        assertTrue(userDataRepository.userProfile.value.claimedDays.isEmpty())
    }

    @Test
    fun `test complete daily challenge updates best score and awards coins`() {
        val initialCoins = userDataRepository.userProfile.value.coins

        userDataRepository.completeDailyChallenge(3500)

        val challenge = userDataRepository.userProfile.value.dailyChallenge
        assertTrue(challenge.isCompleted)
        assertEquals(3500, challenge.bestScore)
        assertEquals(initialCoins + 500, userDataRepository.userProfile.value.coins)
    }
}
