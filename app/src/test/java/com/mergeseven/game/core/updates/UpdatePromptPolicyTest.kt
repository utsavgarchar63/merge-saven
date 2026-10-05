package com.mergeseven.game.core.updates

import org.junit.Assert.*
import org.junit.Test

class UpdatePromptPolicyTest {
    @Test fun `later suppresses same release for a day but a newer release may be offered`() {
        assertFalse(UpdatePromptPolicy.shouldOffer(9, 9, 1000, 1000 + 86_399_999L))
        assertTrue(UpdatePromptPolicy.shouldOffer(9, 9, 1000, 1000 + 86_400_000L))
        assertTrue(UpdatePromptPolicy.shouldOffer(10, 9, 1000, 2000))
        assertFalse(UpdatePromptPolicy.shouldOffer(0, 9, 1000, 2000))
        assertFalse(UpdatePromptPolicy.shouldOffer(9, 9, 1000, 0))
    }
}
