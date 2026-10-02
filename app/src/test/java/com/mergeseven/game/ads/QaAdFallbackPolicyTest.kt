package com.mergeseven.game.ads

import org.junit.Assert.*
import org.junit.Test

class QaAdFallbackPolicyTest {
    @Test fun onlyMarkedDebugDevicesCanFallbackOnce() {
        assertTrue(QaAdFallbackPolicy.allows(true, true, true, true, false))
        assertFalse(QaAdFallbackPolicy.allows(false, true, true, true, false))
        assertFalse(QaAdFallbackPolicy.allows(true, true, true, false, false))
        assertFalse(QaAdFallbackPolicy.allows(true, false, true, true, false))
        assertFalse(QaAdFallbackPolicy.allows(true, true, false, true, false))
        assertFalse(QaAdFallbackPolicy.allows(true, true, true, true, true))
    }
}
