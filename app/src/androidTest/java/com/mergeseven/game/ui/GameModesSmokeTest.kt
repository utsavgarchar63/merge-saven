package com.mergeseven.game.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mergeseven.game.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Exercises actual Compose controls and navigation; engine merge rules have separate tests. */
class GameModesSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allSixModesCanRotatePauseSaveAndResume() = runBlocking {
        val settings = compose.activity.settingsRepository
        val tutorial = settings.isTutorialCompleted.first()
        val guide = settings.isAnimatedGuideSeen.first()
        val motion = settings.isReduceMotionEnabled.first()
        val database = com.mergeseven.game.di.DatabaseModule.provideGameDatabase(compose.activity.applicationContext)
        val dao = database.activeGameDao()
        val slots = listOf("campaign", "endless", "time_attack", "zen", "daily", "weekly")
        val savedSlots = slots.mapNotNull { dao.get(it) }
        try {
            // Existing completed/timed QA saves are not fixtures for a fresh playable run.
            slots.forEach { dao.delete(it) }
            settings.setTutorialCompleted(true); settings.setAnimatedGuideSeen(true); settings.setReduceMotionEnabled(true)
            for (mode in listOf("Campaign", "Endless", "Time Attack", "Zen", "Daily Puzzle", "Weekly")) {
                android.util.Log.i("GameModesQA", "Starting mode: $mode")
                compose.waitUntil(20_000) { compose.onAllNodesWithText("Merge Seven").fetchSemanticsNodes().isNotEmpty() }
                if (compose.onAllNodesWithText("Explore all six modes").fetchSemanticsNodes().isNotEmpty())
                    compose.onNodeWithText("Explore all six modes").performScrollTo().performClick()
                compose.onNodeWithText(mode).performScrollTo().performClick()
                if (mode == "Campaign") {
                    compose.onNodeWithText("1").performClick()
                    compose.onNodeWithText("START LEVEL").performClick()
                } else if (mode == "Daily Puzzle") {
                    val label = if (compose.onAllNodesWithText("Play today's puzzle").fetchSemanticsNodes().isNotEmpty())
                        "Play today's puzzle" else "Practice today's puzzle"
                    compose.onNodeWithText(label).performScrollTo().performClick()
                }
                compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Tray slot 1").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithContentDescription("Tray slot 1").performClick()
                compose.onNodeWithText("ROTATE").performClick()
                saveAndExit()
                android.util.Log.i("GameModesQA", "Verified rotate/pause/save/resume: $mode")
                compose.onNodeWithText("Resume game").performScrollTo().performClick()
                compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Tray slot 1").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithContentDescription("Tray slot 1").assertIsDisplayed()
                compose.onNodeWithText("Take a breath").assertDoesNotExist()
                compose.onNodeWithText("ROTATE").performClick()
                saveAndExit()
            }
        } finally {
            compose.activityRule.scenario.close()
            // Let the stopped ViewModel's final async save settle before restoring QA fixtures.
            kotlinx.coroutines.delay(1_000)
            slots.forEach { dao.delete(it) }
            savedSlots.forEach { dao.upsert(it) }
            database.close()
            settings.setTutorialCompleted(tutorial); settings.setAnimatedGuideSeen(guide); settings.setReduceMotionEnabled(motion)
        }
    }

    private fun saveAndExit() {
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Take a breath").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Take a breath").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save and go Home").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Merge Seven").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isEmpty()
        }
    }
}
