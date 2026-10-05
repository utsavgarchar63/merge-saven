package com.mergeseven.game.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mergeseven.game.MainActivity
import com.mergeseven.game.data.model.DailyGifts
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DailyGiftTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun oneFreeGiftPerDayPersistsAfterActivityRecreation() = runBlocking {
        val repo = compose.activity.userDataRepository
        repo.awaitReady()
        val original = repo.userProfile.value
        val today = java.time.LocalDate.now().toString()
        try {
            repo.restoreProfile(original.copy(lastLoginDate = today, currentStreak = 1, claimedDays = emptySet(),
                rewardClaims = original.rewardClaims.filterKeys { !it.endsWith(":daily_gift") } + (DailyGifts.MIGRATION_KEY to 0)), clearClaims = true)
            repo.flush()
            compose.onNodeWithText("Challenges").performClick()
            compose.onNodeWithText("Claim free").performScrollTo().performClick()
            compose.waitUntil(5_000) { repo.coins() == original.coins + 50 }
            compose.onNodeWithText("Claim free").assertDoesNotExist()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(5_000) { compose.activity.userDataRepository.rewardClaimed(DailyGifts.claimKey(today)) }
            assertEquals(original.coins + 50, compose.activity.userDataRepository.coins())
            assertNull(DailyGifts.available(compose.activity.userDataRepository.userProfile.value))
        } finally { repo.restoreProfile(original, clearClaims = true); repo.flush() }
    }
}
