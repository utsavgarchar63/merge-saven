package com.mergeseven.game.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mergeseven.game.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AnimatedGuideTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun firstPlayGuideResumesItsPageAndStaysDismissedAfterRecreation() {
        val settings = compose.activity.settingsRepository
        val previousMotion = runBlocking { settings.isReduceMotionEnabled.first() }
        try {
            runBlocking { settings.setReduceMotionEnabled(true); settings.resetTutorialGuide() }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Choose a campaign level").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Choose a campaign level").performClick()
            compose.onNodeWithText("1").performClick()
            compose.onNodeWithText("START LEVEL").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Place your first piece").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Next").performClick()
            compose.waitUntil { runBlocking { settings.animatedGuidePage.first() == 1 } }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Rotate for a better fit").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Next").performClick()
            compose.onNodeWithText("Let's play").performClick()
            compose.waitUntil { runBlocking { settings.isAnimatedGuideSeen.first() } }
            compose.activityRule.scenario.recreate()
            assertTrue(runBlocking { settings.isAnimatedGuideSeen.first() })
            compose.onNodeWithText("Make your first merge").assertDoesNotExist()
        } finally {
            runBlocking { settings.setReduceMotionEnabled(previousMotion); settings.setTutorialCompleted(true) }
        }
    }
}
