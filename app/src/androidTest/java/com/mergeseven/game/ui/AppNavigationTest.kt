package com.mergeseven.game.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mergeseven.game.MainActivity
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun primaryDestinationsStayUsableWithoutPurchases() {
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Merge Seven").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Rewards").performClick()
        compose.onNodeWithText("Booster collection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Your collection").assertIsDisplayed()
        compose.onNodeWithText("Challenges").performClick()
        compose.onNodeWithText("Today's puzzle").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Merge Seven").assertIsDisplayed()
    }

    @Test fun localQaInventoryUsesGoogleTestAppAndUnits() {
        assertTrue("This local test suite expects profile=test in admob.properties", BuildConfig.TEST_ADS)
        for (resource in listOf(R.string.admob_app_id, R.string.admob_banner_unit_id,
            R.string.admob_rewarded_unit_id, R.string.admob_interstitial_unit_id)) {
            assertTrue(compose.activity.getString(resource).startsWith("ca-app-pub-3940256099942544"))
        }
    }
}
