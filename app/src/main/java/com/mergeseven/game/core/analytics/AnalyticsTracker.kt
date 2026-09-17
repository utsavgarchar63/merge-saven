package com.mergeseven.game.core.analytics

/**
 * Telemetry boundary. AF0 ships a no-op; AF6 swaps in Firebase without renaming call sites.
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    fun setUserProperty(name: String, value: String?)
}
