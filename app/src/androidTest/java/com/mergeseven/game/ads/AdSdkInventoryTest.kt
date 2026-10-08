package com.mergeseven.game.ads

import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.ConsentInformation
import com.google.android.gms.ads.AdRequest
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit online demo QA. Never requests real inventory or clears game data. */
@RunWith(AndroidJUnit4::class)
class AdSdkInventoryTest {
    /** Run explicitly before manual fresh-production QA; skipped by the regular suite. */
    @Test fun resetConsentForExplicitManualQa() {
        assumeTrue("Explicit debug QA only", BuildConfig.DEBUG &&
            InstrumentationRegistry.getArguments().getString("resetConsentOnly") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("Only SDK-marked test devices", AdRequest.Builder().build().isTestDevice(context))
        instrumentation.runOnMainSync {
            val consent = UserMessagingPlatform.getConsentInformation(context)
            consent.reset()
            assertEquals(ConsentInformation.ConsentStatus.UNKNOWN, consent.consentStatus)
            assertFalse(consent.canRequestAds())
        }
    }

    @Test fun demoInventoryLoadsAfterFreshSdkConsent() {
        assumeTrue("Run with -PadmobProfile=test on a QA device", BuildConfig.DEBUG && BuildConfig.TEST_ADS)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync { UserMessagingPlatform.getConsentInformation(context).reset() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var consent: ConsentManager
            lateinit var ads: AdService
            scenario.onActivity { activity ->
                consent = activity.consentManager
                ads = ViewModelProvider(activity)[BannerAdViewModel::class.java].adService
            }
            await("fresh consent allows demo requests") { consent.canRequestAds.value }
            ads.preload(AdPlacement.FUNDS_COINS)
            ads.preload(AdPlacement.INTERSTITIAL)
            await("rewarded, interstitial and Home banner are loaded") {
                listOf(AdPlacement.FUNDS_COINS, AdPlacement.INTERSTITIAL, AdPlacement.BANNER)
                    .all(ads::isReady)
            }
            fun bannerWidth(): Int {
                var width = 0
                scenario.onActivity { activity ->
                    fun visit(view: android.view.View) {
                        if (view is com.google.android.gms.ads.AdView) width = view.adSize?.width ?: 0
                        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
                    }
                    visit(activity.window.decorView)
                }
                return width
            }
            val portraitWidth = bannerWidth()
            assertTrue(portraitWidth > 0)
            var originalOrientation = 0
            try {
                scenario.onActivity { activity ->
                    originalOrientation = activity.requestedOrientation
                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                await("landscape gets newly sized banner inventory") {
                    bannerWidth() > portraitWidth && ads.isReady(AdPlacement.BANNER)
                }
            } finally {
                scenario.onActivity { it.requestedOrientation = originalOrientation }
            }
        }
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 90_000L
        while (!condition() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
        assertTrue(message, condition())
    }
}
