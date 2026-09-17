package com.mergeseven.game.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAnalyticsTrackerTest {

    @Test
    fun forwardsEventsToBackend() {
        val backend = RecordingBackend()
        val tracker = FirebaseAnalyticsTracker(backend)
        tracker.logEvent(AnalyticsEvents.GAME_START, mapOf("mode" to "campaign", "level" to 1))
        tracker.setUserProperty("tier", "gold")

        assertEquals(1, backend.events.size)
        assertEquals(AnalyticsEvents.GAME_START, backend.events[0].first)
        assertEquals("campaign", backend.events[0].second["mode"])
        assertEquals("gold", backend.props["tier"])
    }

    private class RecordingBackend : AnalyticsBackend {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        val props = mutableMapOf<String, String?>()

        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }

        override fun setUserProperty(name: String, value: String?) {
            props[name] = value
        }
    }
}
