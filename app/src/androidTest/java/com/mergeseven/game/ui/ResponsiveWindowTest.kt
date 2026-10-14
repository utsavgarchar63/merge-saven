package com.mergeseven.game.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mergeseven.game.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Run on phone/tablet windows in each orientation; checks real controls and board bounds. */
class ResponsiveWindowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun pagesAndGameRemainUsableAtCurrentWindowSize(): Unit = runBlocking {
        val settings = compose.activity.settingsRepository
        val tutorial = settings.isTutorialCompleted.first()
        val guide = settings.isAnimatedGuideSeen.first()
        val motion = settings.isReduceMotionEnabled.first()
        val database = com.mergeseven.game.di.DatabaseModule.provideGameDatabase(compose.activity.applicationContext)
        val dao = database.activeGameDao()
        val oldZen = dao.get("zen")
        try {
            dao.delete("zen")
            settings.setTutorialCompleted(true); settings.setAnimatedGuideSeen(true); settings.setReduceMotionEnabled(true)
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Merge Seven").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Merge Seven. Place. Match. Merge.").assertIsDisplayed()
            for (tab in listOf("Home", "Challenges", "Rewards", "Profile")) {
                compose.onNodeWithText(tab).assertIsDisplayed()
            }
            capture("home")
            for (mode in listOf("Campaign", "Endless", "Time Attack", "Zen", "Daily Puzzle", "Weekly")) {
                compose.onNodeWithText(mode).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText("Rewards").performClick()
            compose.onNodeWithText("Booster collection").performScrollTo().assertIsDisplayed()
            capture("rewards")
            compose.onNodeWithText("Cosmetics").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Challenges").performClick()
            compose.onNodeWithText("Your daily gifts").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Day 1").performScrollTo().assertIsDisplayed()
            capture("challenges")
            compose.onNodeWithText("Daily quests").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Profile").performClick()
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Replay how to play").performScrollTo().assertIsDisplayed()
            capture("settings")
            back()
            compose.onNodeWithText("Cosmetics").performScrollTo().performClick()
            compose.onNodeWithText("Tiles").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Boards").assertIsDisplayed()
            capture("cosmetics")
            back()
            compose.onNodeWithText("Home").performClick()
            compose.onNodeWithText("Choose a campaign level").performScrollTo().performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("LEVEL ROADMAP").assertIsDisplayed()
            compose.onNodeWithText("1").assertIsDisplayed()
            capture("levels")
            back()
            compose.onNodeWithText("Zen").performScrollTo().performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("game_board").fetchSemanticsNodes().isNotEmpty() }
            for (slot in 1..3) compose.onNodeWithContentDescription("Tray slot $slot").assertIsDisplayed()
            val board = compose.onNodeWithTag("game_board").fetchSemanticsNode().boundsInRoot
            val density = compose.activity.resources.displayMetrics.density
            assertTrue("Board keeps playable space: $board", board.width / density >= 120 && board.height / density >= 120)
            compose.onNodeWithContentDescription("Tray slot 1").performClick()
            compose.onNodeWithText("ROTATE").performClick()
            capture("game")
            compose.onNodeWithContentDescription("Pause").performClick()
            compose.onNodeWithText("Save and go Home").performScrollTo().assertIsDisplayed()
            capture("pause")
            compose.onNodeWithText("Save and go Home").performClick()
            compose.onNodeWithText("Merge Seven").assertIsDisplayed()
            compose.onNodeWithText("Resume game").performScrollTo().performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("game_board").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tray slot 1").assertIsDisplayed()
        } finally {
            compose.activityRule.scenario.close()
            kotlinx.coroutines.delay(1_000)
            dao.delete("zen"); oldZen?.let { dao.upsert(it) }; database.close()
            settings.setTutorialCompleted(tutorial); settings.setAnimatedGuideSeen(guide); settings.setReduceMotionEnabled(motion)
        }
    }

    private fun back() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

    private fun capture(screen: String) {
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: return
        require(prefix.matches(Regex("[a-z0-9_-]+")))
        compose.waitForIdle()
        val target = File(compose.activity.getExternalFilesDir(null), "responsive-qa/$prefix-$screen.png")
        target.parentFile?.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val arguments = InstrumentationRegistry.getArguments()
        arguments.getString("expectedPixelWidth")?.toInt()?.let { assertEquals("Actual QA display width", it, bitmap.width) }
        arguments.getString("expectedPixelHeight")?.toInt()?.let { assertEquals("Actual QA display height", it, bitmap.height) }
        target.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
