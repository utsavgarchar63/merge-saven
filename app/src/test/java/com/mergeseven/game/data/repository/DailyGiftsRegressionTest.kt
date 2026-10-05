package com.mergeseven.game.data.repository

import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.data.model.DailyGifts
import com.mergeseven.game.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyGiftsRegressionTest {
    private var date = "2026-10-05"
    private fun repo(store: InMemoryUserProfileStore = InMemoryUserProfileStore()) = UserDataRepository(
        store, CoroutineScope(UnconfinedTestDispatcher()), DateProvider { date }, InMemoryFeatureFlags(isDebug = true))
        .also { it.checkDailyLogin() }

    @Test fun `yesterdays unfinished daily board cannot claim todays reward or overwrite score`() {
        val repository = repo()
        val oldDate = date
        date = "2026-10-06"
        repository.checkDailyLogin()
        repository.finishDailyAttempt(3500, expectedDate = oldDate)
        assertEquals(100, repository.coins())
        assertEquals(0, repository.userProfile.value.dailyChallenge.attempts)
        assertEquals(0, repository.userProfile.value.dailyChallenge.bestScore)
        repository.finishDailyAttempt(3500, expectedDate = date)
        repository.finishDailyAttempt(5000, expectedDate = oldDate)
        assertEquals(600, repository.coins())
        assertEquals(3500, repository.userProfile.value.dailyChallenge.bestScore)
        assertEquals(1, repository.userProfile.value.dailyChallenge.attempts)
    }

    @Test fun `background hydration alone does not record a login or extend a cycle`() {
        val original = UserProfile(lastLoginDate = "2026-10-01", currentStreak = 4, claimedDays = setOf(1, 2, 3))
        val store = InMemoryUserProfileStore(original)
        val background = UserDataRepository(store, CoroutineScope(UnconfinedTestDispatcher()), DateProvider { date },
            InMemoryFeatureFlags(isDebug = true))
        assertEquals(original, background.userProfile.value)
        assertEquals(original, kotlinx.coroutines.runBlocking { store.load() })
        background.checkDailyLogin()
        assertEquals(date, background.userProfile.value.lastLoginDate)
        assertEquals(1, background.userProfile.value.currentStreak)
    }

    @Test fun `duplicate and next day taps do not drain the whole cycle`() {
        val store = InMemoryUserProfileStore()
        val repository = repo(store)
        assertTrue(repository.claimDailyReward(1, 50, 0))
        assertFalse(repository.claimDailyReward(1, 50, 0))
        assertFalse(repository.claimDailyReward(2, 100, 0))
        val restarted = repo(store)
        assertEquals(150, restarted.coins())
        assertNull(DailyGifts.available(restarted.userProfile.value))
        date = "2026-10-06"
        restarted.checkDailyLogin()
        assertEquals(2, DailyGifts.available(restarted.userProfile.value)?.day)
        assertTrue(restarted.claimDailyReward(2, 100, 0))
        assertEquals(250, repo(store).coins())
    }

    @Test fun `forged amounts and skipped future gifts pay nothing`() {
        val repository = repo()
        assertFalse(repository.claimDailyReward(1, 1000, 10))
        assertFalse(repository.claimDailyReward(7, 1000, 10))
        assertFalse(repository.claimDailyReward(0, 50, 0))
        assertEquals(100, repository.coins())
        assertTrue(repository.claimDailyReward(1, 50, 0))
    }

    @Test fun `all seven gifts require separate days then cycle restarts`() {
        val repository = repo()
        DailyGifts.rewards.forEach { gift ->
            date = java.time.LocalDate.of(2026, 10, 4).plusDays(gift.day.toLong()).toString()
            repository.checkDailyLogin()
            assertTrue(repository.claimDailyReward(gift.day, gift.coins, gift.stars))
            assertFalse(repository.claimDailyReward(gift.day, gift.coins, gift.stars))
        }
        assertEquals(2400, repository.coins())
        assertEquals(20, repository.userProfile.value.totalStars)
        date = "2026-10-12"
        repository.checkDailyLogin()
        assertEquals(1, DailyGifts.available(repository.userProfile.value)?.day)
        assertTrue(repository.claimDailyReward(1, 50, 0))
    }

    @Test fun `legacy claim upgrade keeps wallet and prevents another gift on same date`() {
        val store = InMemoryUserProfileStore(UserProfile(coins = 700, currentStreak = 3,
            claimedDays = setOf(1, 2), lastLoginDate = date))
        val repository = repo(store)
        assertEquals(700, repository.coins())
        assertFalse(repository.claimDailyReward(3, 150, 2))
        date = "2026-10-06"
        repository.checkDailyLogin()
        assertTrue(repository.claimDailyReward(3, 150, 2))
        assertEquals(850, repo(store).coins())
    }

    @Test fun `missed day resets gifts but backwards clock cannot reopen claims`() {
        val repository = repo()
        assertTrue(repository.claimDailyReward(1, 50, 0))
        date = "2026-10-08"
        repository.checkDailyLogin()
        assertTrue(repository.claimDailyReward(1, 50, 0))
        date = "2026-10-06"
        repository.checkDailyLogin()
        assertFalse(repository.claimDailyReward(1, 50, 0))
        assertFalse(repository.claimDailyReward(2, 100, 0))
        assertEquals("2026-10-08", repository.userProfile.value.lastLoginDate)
        assertEquals(200, repository.coins())
    }
}
