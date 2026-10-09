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
        gate.failed(AdPlacement.FREE_HINT, 1_000L)
        gate.failed(AdPlacement.INTERSTITIAL, 1_000L)
        gate.clear()
        assertTrue(gate.tryBegin(AdPlacement.FUNDS_COINS, 1_001L))
        assertTrue(gate.tryBegin(AdPlacement.INTERSTITIAL, 1_001L))
    }
    @Test fun failureBackoffGrowsAndKeepsRecoveringAfterThreeFailures() {
        val gate = AdLoadGate()
        var now = 1_000L
        listOf(30_000L, 60_000L, 120_000L, 240_000L, 300_000L, 300_000L).forEach { delay ->
            assertTrue(gate.tryBegin(AdPlacement.FUNDS_COINS, now))
            assertEquals(delay, gate.failed(AdPlacement.FUNDS_COINS, now))
            assertFalse(gate.tryBegin(AdPlacement.CONTINUE, now + delay - 1))
            now += delay
        }
        assertTrue(gate.tryBegin(AdPlacement.FREE_HINT, now))
    }
    @Test fun foregroundChecksAndRewardClicksCannotBypassFailureDelay() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.FUNDS_COINS, 1_000L)
        gate.failed(AdPlacement.FUNDS_COINS, 11_000L)
        assertFalse(gate.tryBegin(AdPlacement.EXTRA_DAILY, 31_000L))
        assertTrue(gate.tryBegin(AdPlacement.CONTINUE, 41_000L))
        assertFalse(gate.tryBegin(AdPlacement.FUNDS_COINS, 41_000L))
        assertTrue(gate.tryBegin(AdPlacement.INTERSTITIAL, 41_000L))
    }
    @Test fun successfulInventoryResetsFailureBackoffForNextReward() {
        val gate = AdLoadGate()
        gate.tryBegin(AdPlacement.FUNDS_COINS, 1_000L)
        gate.failed(AdPlacement.FUNDS_COINS, 1_000L)
        gate.tryBegin(AdPlacement.CONTINUE, 31_000L)
        gate.failed(AdPlacement.CONTINUE, 31_000L)
        gate.tryBegin(AdPlacement.FREE_HINT, 91_000L)
        gate.finished(AdPlacement.FREE_HINT)
        gate.allowRetry(AdPlacement.FREE_HINT)
        assertTrue(gate.tryBegin(AdPlacement.DOUBLE_COINS, 91_001L))
        assertEquals(30_000L, gate.failed(AdPlacement.DOUBLE_COINS, 91_001L))
    }
}
