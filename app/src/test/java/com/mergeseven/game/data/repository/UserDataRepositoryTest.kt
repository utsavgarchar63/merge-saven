package com.mergeseven.game.data.repository

import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.testing.TestPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The point of these tests is durability: a new repository built on the same store must see what
 * the previous one wrote. That is what "restarting the app" means here.
 */
class UserDataRepositoryTest {

    @Test
    fun `coins survive a restart`() {
        val store = InMemoryUserProfileStore()

        TestPersistence.userDataRepository(store).addCoins(500)

        val afterRestart = TestPersistence.userDataRepository(store)
        assertEquals(UserProfile.STARTING_COINS + 500, afterRestart.userProfile.value.coins)
    }

    @Test
    fun `stars survive a restart`() {
        val store = InMemoryUserProfileStore()

        TestPersistence.userDataRepository(store).addStars(7)

        assertEquals(7, TestPersistence.userDataRepository(store).userProfile.value.totalStars)
    }

    @Test
    fun `claimed reward days survive a restart`() {
        val store = InMemoryUserProfileStore()

        TestPersistence.userDataRepository(store).claimDailyReward(day = 1, coinsReward = 50, starsReward = 0)

        val profile = TestPersistence.userDataRepository(store).userProfile.value
        assertTrue(profile.claimedDays.contains(1))
        assertEquals(UserProfile.STARTING_COINS + 50, profile.coins)
    }

    @Test
    fun `quest progress and claim state survive a restart`() {
        val store = InMemoryUserProfileStore()

        with(TestPersistence.userDataRepository(store)) {
            updateQuestProgress("quest_merge", 10)
            claimQuestReward("quest_merge")
        }

        val quest = TestPersistence.userDataRepository(store)
            .userProfile.value.dailyQuests.first { it.id == "quest_merge" }
        assertTrue(quest.isClaimed)
        assertEquals(10, quest.currentProgress)
    }

    @Test
    fun `daily challenge best score survives a restart`() {
        val store = InMemoryUserProfileStore()

        TestPersistence.userDataRepository(store).completeDailyChallenge(3_500)

        val challenge = TestPersistence.userDataRepository(store).userProfile.value.dailyChallenge
        assertEquals(3_500, challenge.bestScore)
        assertTrue(challenge.isCompleted)
        assertEquals(1, challenge.attempts)
    }

    @Test
    fun `first run starts from the seed values`() {
        val repository = TestPersistence.userDataRepository(InMemoryUserProfileStore())

        val profile = repository.userProfile.value
        assertEquals(UserProfile.STARTING_COINS, profile.coins)
        assertEquals(0, profile.totalStars)
        assertEquals(1, profile.currentStreak)
        assertEquals(3, profile.dailyQuests.size)
    }

    @Test
    fun `a reward cannot be claimed twice`() {
        val repository = TestPersistence.userDataRepository()

        repository.claimDailyReward(day = 1, coinsReward = 50, starsReward = 0)
        repository.claimDailyReward(day = 1, coinsReward = 50, starsReward = 0)

        assertEquals(UserProfile.STARTING_COINS + 50, repository.userProfile.value.coins)
    }

    @Test
    fun `an incomplete quest pays nothing`() {
        val repository = TestPersistence.userDataRepository()

        repository.updateQuestProgress("quest_merge", 3)
        repository.claimQuestReward("quest_merge")

        val quest = repository.userProfile.value.dailyQuests.first { it.id == "quest_merge" }
        assertFalse(quest.isClaimed)
        assertEquals(UserProfile.STARTING_COINS, repository.userProfile.value.coins)
    }

    @Test
    fun `a quest pays out only once`() {
        val repository = TestPersistence.userDataRepository()

        repository.updateQuestProgress("quest_merge", 10)
        repository.claimQuestReward("quest_merge")
        repository.claimQuestReward("quest_merge")

        assertEquals(UserProfile.STARTING_COINS + 100, repository.userProfile.value.coins)
    }

    @Test
    fun `replaying the daily challenge does not pay twice`() {
        val repository = TestPersistence.userDataRepository()

        repository.completeDailyChallenge(3_500)
        val afterFirst = repository.userProfile.value.coins
        repository.completeDailyChallenge(4_000)

        assertEquals(afterFirst, repository.userProfile.value.coins)
        assertEquals(3_500, repository.userProfile.value.dailyChallenge.bestScore)
        assertEquals(2, repository.userProfile.value.dailyChallenge.attempts)
    }

    @Test
    fun `a day rollover resets quests but keeps the wallet`() {
        val store = InMemoryUserProfileStore()
        val repository = TestPersistence.userDataRepository(store, today = "2026-01-01")

        repository.addCoins(300)
        repository.updateQuestProgress("quest_merge", 10)
        repository.checkDailyLogin("2026-01-02")

        val profile = repository.userProfile.value
        assertEquals(UserProfile.STARTING_COINS + 300, profile.coins)
        assertEquals(0, profile.dailyQuests.first { it.id == "quest_merge" }.currentProgress)
        assertEquals("2026-01-02", profile.dailyChallenge.dateSeed)
    }

    @Test
    fun `a missed day resets the streak`() {
        val repository = TestPersistence.userDataRepository(today = "2026-01-01")

        repository.claimDailyReward(day = 1, coinsReward = 50, starsReward = 0)
        repository.checkDailyLogin("2026-01-05")

        val profile = repository.userProfile.value
        assertEquals(1, profile.currentStreak)
        assertTrue(profile.claimedDays.isEmpty())
    }

    @Test
    fun `consecutive days keep the claim history`() {
        val repository = TestPersistence.userDataRepository(today = "2026-01-01")

        repository.claimDailyReward(day = 1, coinsReward = 50, starsReward = 0)
        repository.checkDailyLogin("2026-01-02")

        assertTrue(repository.userProfile.value.claimedDays.contains(1))
        assertEquals(2, repository.userProfile.value.currentStreak)
    }

    @Test
    fun `stored profile is not overwritten by the default one`() {
        val store = InMemoryUserProfileStore(UserProfile(coins = 9_999, totalStars = 42))

        val repository = TestPersistence.userDataRepository(store)

        assertEquals(9_999, repository.userProfile.value.coins)
        assertEquals(42, repository.userProfile.value.totalStars)
    }

    @Test
    fun `trySpendCoins rejects overdraft and never goes negative`() {
        val repository = TestPersistence.userDataRepository()
        val start = repository.coins()

        assertFalse(repository.trySpendCoins(start + 1))
        assertEquals(start, repository.coins())

        assertTrue(repository.trySpendCoins(start))
        assertEquals(0, repository.coins())
        assertFalse(repository.trySpendCoins(1))
        assertEquals(0, repository.coins())
    }

    @Test
    fun `trySpendCoins with zero amount is a no-op success`() {
        val repository = TestPersistence.userDataRepository()
        val start = repository.coins()
        assertTrue(repository.trySpendCoins(0))
        assertEquals(start, repository.coins())
    }
}
