package com.mergeseven.game.ads

import org.junit.Assert.*
import org.junit.Test

class AdLoadRecoveryTest {
    @Test fun loadingAndFailureAreSharedByEveryRewardOffer() {
        val store = AdLoadStateStore()
        store.set(AdPlacement.CONTINUE, AdLoadState.LOADING)
        rewardedPlacements.forEach { assertEquals(AdLoadState.LOADING, store.flow.value[it]) }
        assertEquals(AdLoadState.CHECKING_PRIVACY, store.flow.value[AdPlacement.BANNER])
        store.set(AdPlacement.FREE_HINT, AdLoadState.UNAVAILABLE)
        rewardedPlacements.forEach { assertEquals(AdLoadState.UNAVAILABLE, store.flow.value[it]) }
    }

    @Test fun consentWithdrawalClearsReadyStatesAcrossFormats() {
        val store = AdLoadStateStore()
        store.set(AdPlacement.FUNDS_COINS, AdLoadState.READY)
        store.set(AdPlacement.BANNER, AdLoadState.READY)
        store.reset(AdLoadState.PRIVACY_REQUIRED)
        AdPlacement.entries.forEach { assertEquals(AdLoadState.PRIVACY_REQUIRED, store.flow.value[it]) }
        store.set(AdPlacement.DOUBLE_COINS, AdLoadState.READY)
        rewardedPlacements.forEach { assertEquals(AdLoadState.READY, store.flow.value[it]) }
        assertEquals(AdLoadState.PRIVACY_REQUIRED, store.flow.value[AdPlacement.BANNER])
    }

    @Test fun unavailableInventoryNeverPromisesToWatchAnAd() {
        val offer = "Watch ad for coins"
        AdLoadState.entries.forEach {
            assertNotEquals(offer, it.actionLabel(false, offer))
            assertEquals(offer, it.actionLabel(true, offer))
        }
        assertTrue(AdLoadState.UNAVAILABLE.canRequest)
        assertTrue(AdLoadState.PRIVACY_REQUIRED.canRequest)
        assertFalse(AdLoadState.LOADING.canRequest)
        assertFalse(AdLoadState.CHECKING_PRIVACY.canRequest)
        assertFalse(AdLoadState.DISABLED.canRequest)
    }

    @Test fun explicitRetryCannotDuplicateALoadOrSkipMinimumInterval() {
        val gate = AdLoadGate()
        assertTrue(gate.tryBegin(AdPlacement.FUNDS_COINS, 1_000))
        assertFalse(gate.requestRetry(AdPlacement.FREE_HINT, 40_000))
        gate.failed(AdPlacement.FUNDS_COINS, 2_000)
        assertFalse(gate.requestRetry(AdPlacement.CONTINUE, 30_999))
        assertTrue(gate.requestRetry(AdPlacement.CONTINUE, 31_000))
        assertTrue(gate.tryBegin(AdPlacement.CONTINUE, 31_000))
        assertFalse(gate.tryBegin(AdPlacement.DOUBLE_COINS, 31_001))
    }

    @Test fun explicitRetryDoesNotResetRepeatedFailureBackoff() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.FUNDS_COINS, 1_000)
        assertEquals(30_000L, gate.failed(AdPlacement.FUNDS_COINS, 2_000))
        assertTrue(gate.requestRetry(AdPlacement.FUNDS_COINS, 31_000))
        gate.tryBegin(AdPlacement.FUNDS_COINS, 31_000)
        assertEquals(60_000L, gate.failed(AdPlacement.FUNDS_COINS, 32_000))
    }
}
