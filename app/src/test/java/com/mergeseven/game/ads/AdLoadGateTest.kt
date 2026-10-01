package com.mergeseven.game.ads

import org.junit.Assert.*
import org.junit.Test

class AdLoadGateTest {
    @Test fun differentRewardOffersCannotStartDuplicateSdkLoads() {
        val gate = AdLoadGate()
        assertTrue(gate.tryBegin(AdPlacement.FUNDS_COINS, 1_000L))
        assertFalse(gate.tryBegin(AdPlacement.FREE_HINT, 1_001L))
        assertFalse(gate.tryBegin(AdPlacement.CONTINUE, 1_002L))
        assertTrue(gate.tryBegin(AdPlacement.INTERSTITIAL, 1_003L))
    }
    @Test fun failureBackoffCannotBeBypassedByAnotherRewardButton() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.FREE_HINT, 1_000L)
        gate.finished(AdPlacement.FREE_HINT)
        assertFalse(gate.tryBegin(AdPlacement.DOUBLE_COINS, 30_999L))
        assertTrue(gate.tryBegin(AdPlacement.DOUBLE_COINS, 31_000L))
    }
    @Test fun consumedInventoryCanReloadWithoutWaitingForFailureBackoff() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.EXTRA_DAILY, 1_000L)
        gate.finished(AdPlacement.EXTRA_DAILY)
        gate.allowRetry(AdPlacement.FUNDS_COINS)
        assertTrue(gate.tryBegin(AdPlacement.CONTINUE, 1_001L))
    }
    @Test fun consentInvalidationClearsLoadingAndBackoffForAllFormats() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.FREE_HINT, 1_000L)
        gate.tryBegin(AdPlacement.INTERSTITIAL, 1_000L)
        gate.clear()
        assertTrue(gate.tryBegin(AdPlacement.FUNDS_COINS, 1_001L))
        assertTrue(gate.tryBegin(AdPlacement.INTERSTITIAL, 1_001L))
    }
}
