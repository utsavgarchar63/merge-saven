package com.mergeseven.game.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mergeseven.game.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class NavigationRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun rewardsChallengesLinkReturnsToHomeWithoutStackingTabs() {
        repeat(2) {
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Merge Seven").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Rewards").performClick()
            compose.onNodeWithText("Daily gifts and quests").performScrollTo().performClick()
            compose.onNodeWithText("Your daily gifts").performScrollTo().assertIsDisplayed()
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Merge Seven").assertIsDisplayed()
            compose.onNodeWithText("Booster collection").assertDoesNotExist()
        }
    }

    @Test fun systemBackPausesAndResumesInsteadOfAbandoningTheBoard(): Unit = runBlocking {
        val settings = compose.activity.settingsRepository
        val tutorial = settings.isTutorialCompleted.first()
        val guide = settings.isAnimatedGuideSeen.first()
        val database = com.mergeseven.game.di.DatabaseModule.provideGameDatabase(compose.activity.applicationContext)
        val dao = database.activeGameDao()
        val oldZen = dao.get("zen")
        try {
            settings.setTutorialCompleted(true); settings.setAnimatedGuideSeen(true)
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Zen").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Zen").performScrollTo().performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Tray slot 1").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Take a breath").assertIsDisplayed()
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Take a breath").assertDoesNotExist()
            compose.onNodeWithContentDescription("Tray slot 1").assertIsDisplayed()
            compose.onNodeWithContentDescription("Pause").performClick()
            compose.onNodeWithText("Save and go Home").performScrollTo().performClick()
            compose.onNodeWithText("Merge Seven").assertIsDisplayed()
        } finally {
            compose.activityRule.scenario.close()
            kotlinx.coroutines.delay(1_000)
            dao.delete("zen")
            oldZen?.let { dao.upsert(it) }
            database.close()
            settings.setTutorialCompleted(tutorial); settings.setAnimatedGuideSeen(guide)
        }
    }
}
