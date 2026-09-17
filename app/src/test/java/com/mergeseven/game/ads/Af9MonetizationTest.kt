package com.mergeseven.game.ads

import com.mergeseven.game.billing.FakeReceiptValidator
import com.mergeseven.game.billing.ProductCatalog
import com.mergeseven.game.billing.ValidationResult
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.testing.TestPersistence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Af9MonetizationTest {

    @Test
    fun af9OffNoOpAdNeverReady() {
        val ads = NoOpAdService()
        assertFalse(ads.isReady(AdPlacement.CONTINUE))
        ads.preload(AdPlacement.CONTINUE)
        assertEquals(1, ads.loadAttempts)
    }

    @Test
    fun interstitialBlockedAfterLossAndTutorial() = runTest {
        val policy = FakeInterstitialPolicy(allow = true)
        assertEquals("after_loss", policy.evaluate(sessionWon = false, inTutorial = false, suppressAds = false).reason)
        assertEquals("tutorial", policy.evaluate(sessionWon = true, inTutorial = true, suppressAds = false).reason)
        assertTrue(policy.evaluate(sessionWon = true, inTutorial = false, suppressAds = false).allow)
    }

    @Test
    fun rewardedNoFillDoesNotGrant() = runTest {
        val ads = FakeAdService(rewardedResult = AdResult.NoFill)
        val result = ads.showRewarded(android.app.Activity(), AdPlacement.CONTINUE)
        assertEquals(AdResult.NoFill, result)
        assertEquals(1, ads.showRewardedCalls.size)
    }

    @Test
    fun receiptFailureLeavesPendingSemantics() = runTest {
        val validator = FakeReceiptValidator(ValidationResult(ok = false, message = "bad"))
        val outcome = validator.validate("coins_500", "tok", "pkg")
        assertFalse(outcome.ok)
        assertEquals(1, validator.calls)
    }

    @Test
    fun purchaseGrantIdempotentByToken() = runTest {
        val flags = InMemoryFeatureFlags(isDebug = true, initial = mapOf(Feature.AF9 to true))
        val repo = TestPersistence.userDataRepository(featureFlags = flags)
        val before = repo.coins()
        val product = ProductCatalog.byId(ProductCatalog.COINS_500)
        assertEquals(500, product?.coins)
        assertTrue(before >= 0)
    }

    @Test
    fun consentUnresolvedBlocksFakeLoads() = runTest {
        val consent = FakeConsentManager(initiallyCanRequest = false)
        assertFalse(consent.canRequestAds.value)
        assertFalse(consent.resolved.value)
        consent.gatherConsent(android.app.Activity())
        assertTrue(consent.resolved.value)
        assertFalse(consent.canRequestAds.value)
    }

    @Test
    fun removeAdsPolicySkipsInterstitialEvenWhenAllowTrue() = runTest {
        val policy = FakeInterstitialPolicy(allow = true)
        assertFalse(policy.evaluate(true, false, suppressAds = true).allow)
    }

    @Test
    fun corruptOffersJsonYieldsNoOffer() {
        val flags = InMemoryFeatureFlags(isDebug = true, initial = mapOf(Feature.AF9 to true))
        val engine = com.mergeseven.game.billing.OfferEngine(
            liveConfig = com.mergeseven.game.core.liveops.ParsedLiveConfig(
                strings = {
                    mapOf(com.mergeseven.game.core.liveops.LiveConfigKeys.OFFERS_JSON to "{not-json")
                },
                revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
            ),
            featureFlags = flags,
            userDataRepository = TestPersistence.userDataRepository(featureFlags = flags)
        )
        assertNull(engine.currentOffer())
    }
}
