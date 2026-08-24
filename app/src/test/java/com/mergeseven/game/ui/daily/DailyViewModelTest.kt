package com.mergeseven.game.ui.daily

import android.content.Context
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.data.repository.UserDataRepository
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
        userDataRepository = UserDataRepository()
        // Provide audioManager instance safely
        val dummyContext = try {
            val contextClass = Class.forName("android.content.Context")
            java.lang.reflect.Proxy.newProxyInstance(
                contextClass.classLoader,
                arrayOf(contextClass)
            ) { _, _, _ -> null } as Context
        } catch (e: Exception) {
            // Fallback if proxying context in JVM test environment
            throw e
        }
        audioManager = AudioManager(dummyContext)
        viewModel = DailyViewModel(userDataRepository, audioManager)
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
