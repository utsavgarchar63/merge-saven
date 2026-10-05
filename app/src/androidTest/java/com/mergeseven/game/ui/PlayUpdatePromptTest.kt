package com.mergeseven.game.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.mergeseven.game.core.updates.*
import com.mergeseven.game.ui.components.PlayUpdatePrompt
import com.mergeseven.game.ui.theme.MergeSevenTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlayUpdatePromptTest {
    @get:Rule val compose = createComposeRule()
    private val launcher = object : ActivityResultLauncher<IntentSenderRequest>() {
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract: ActivityResultContract<IntentSenderRequest, *> = ActivityResultContracts.StartIntentSenderForResult()
    }

    @Test fun largeTextKeepsBothActionsInsidePhoneAndShortLandscapeCards() {
        var short by mutableStateOf(false)
        compose.setContent {
            val config = android.content.res.Configuration(LocalConfiguration.current).apply {
                screenHeightDp = if (short) 320 else 533
            }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.8f),
                LocalConfiguration provides config) {
                MergeSevenTheme {
                    Box(Modifier.size(300.dp, if (short) 210.dp else 280.dp).testTag("update_card_frame")) {
                        PlayUpdatePrompt(PlayUpdateState(UpdateStage.AVAILABLE), {}, {}, {})
                    }
                }
            }
        }
        fun assertButtonsFit() {
            val root = compose.onNodeWithTag("update_card_frame").fetchSemanticsNode().boundsInRoot
            for (label in listOf("Later", "Update")) {
                val button = compose.onNodeWithText(label)
                button.assertIsDisplayed()
                val bounds = button.fetchSemanticsNode().boundsInRoot
                assertTrue("$label stays inside the card", bounds.bottom <= root.bottom && bounds.left >= root.left && bounds.right <= root.right)
            }
        }
        assertButtonsFit()
        compose.runOnIdle { short = true }
        assertButtonsFit()
    }

    @Test fun flexibleUpdateDownloadsThenWaitsForExplicitRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("update_qa_only", 0)
        prefs.edit().clear().commit()
        val manager = FakeAppUpdateManager(context)
        val controller = PlayUpdateController(manager, prefs)
        try {
            compose.setContent { MergeSevenTheme {
                val state by controller.state.collectAsState()
                PlayUpdatePrompt(state, { controller.start(launcher) }, controller::finish, controller::dismiss)
            } }
            compose.runOnIdle { manager.setUpdateAvailable(99); controller.check() }
            compose.waitUntil(5_000) { controller.state.value.stage == UpdateStage.AVAILABLE }
            compose.onNodeWithText("Update").performClick()
            compose.waitUntil(5_000) { manager.isConfirmationDialogVisible }
            compose.runOnIdle { manager.userAcceptsUpdate(); manager.downloadStarts() }
            compose.waitUntil(5_000) { controller.state.value.stage == UpdateStage.DOWNLOADING }
            compose.onNodeWithText("Downloading update").assertIsDisplayed()
            compose.runOnIdle { manager.downloadCompletes() }
            compose.waitUntil(5_000) { controller.state.value.stage == UpdateStage.DOWNLOADED }
            compose.onNodeWithText("Restart").assertIsDisplayed()
            assertFalse(manager.isInstallSplashScreenVisible)
            compose.onNodeWithText("Restart").performClick()
            compose.waitUntil(5_000) { manager.isInstallSplashScreenVisible }
        } finally { compose.runOnIdle { controller.close() }; prefs.edit().clear().commit() }
    }

    @Test fun laterSurvivesControllerRestartAndUnavailableUpdatesStayHidden() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("update_qa_later_only", 0)
        prefs.edit().clear().commit()
        val manager = FakeAppUpdateManager(context)
        var controller = PlayUpdateController(manager, prefs) { 100_000L }
        try {
            compose.runOnIdle { manager.setUpdateAvailable(99); controller.check() }
            compose.waitUntil(5_000) { controller.state.value.stage == UpdateStage.AVAILABLE }
            compose.runOnIdle {
                controller.dismiss(); controller.close()
                controller = PlayUpdateController(manager, prefs) { 100_001L }
                controller.check()
            }
            compose.waitForIdle()
            assertEquals(UpdateStage.NONE, controller.state.value.stage)
            compose.runOnIdle { manager.setUpdateNotAvailable(); controller.check() }
            compose.waitForIdle()
            assertEquals(UpdateStage.NONE, controller.state.value.stage)
        } finally { compose.runOnIdle { controller.close() }; prefs.edit().clear().commit() }
    }
}
