package com.mergeseven.game.core.liveops

import com.mergeseven.game.data.model.DailyGifts
import com.mergeseven.game.data.model.UserProfile
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ReminderContentTest {
    private val today = LocalDate.of(2026, 10, 5)
    @Test fun `gift preview after a missed day is accurate and does not change profile`() {
        val player = UserProfile(lastLoginDate = "2026-10-01", currentStreak = 6, claimedDays = (1..5).toSet())
        assertEquals(50, DailyReminderContent.giftCoins(player, today))
        assertTrue(DailyReminderContent.forPlayer(player, today)!!.body.contains("50 free coins"))
        assertEquals("2026-10-01", player.lastLoginDate)
    }
    @Test fun `consecutive day gift preview and completed cycle are accurate`() {
        val player = UserProfile(lastLoginDate = "2026-10-04", currentStreak = 3, claimedDays = setOf(1, 2))
        assertEquals(150, DailyReminderContent.giftCoins(player, today))
        assertEquals(50, DailyReminderContent.giftCoins(player.copy(claimedDays = (1..7).toSet()), today))
    }
    @Test fun `completed puzzle does not hide an available gift but all done suppresses invitation`() {
        val player = UserProfile(lastLoginDate = today.toString(), rewardClaims = mapOf(DailyGifts.MIGRATION_KEY to 0),
            dailyChallenge = com.mergeseven.game.data.model.DailyChallengeState(dateSeed = today.toString(), isCompleted = true))
        assertNotNull(DailyReminderContent.forPlayer(player, today))
        assertNull(DailyReminderContent.forPlayer(player.copy(claimedDays = setOf(1), currentStreak = 2,
            rewardClaims = player.rewardClaims + (DailyGifts.claimKey(today.toString()) to 50)), today))
    }
}
