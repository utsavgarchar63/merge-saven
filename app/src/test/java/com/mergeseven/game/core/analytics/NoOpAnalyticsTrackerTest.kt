package com.mergeseven.game.core.analytics

import org.junit.Test

class NoOpAnalyticsTrackerTest {

    @Test
    fun `noop tracker never throws`() {
        val tracker = NoOpAnalyticsTracker()

        tracker.logEvent(AnalyticsEvents.APP_OPEN)
        tracker.logEvent(AnalyticsEvents.GAME_START, mapOf("level" to 3, "seed" to null))
        tracker.setUserProperty("player_tier", "gold")
        tracker.setUserProperty("player_tier", null)
    }
}
