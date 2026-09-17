package com.mergeseven.game.core.analytics

/**
 * Narrow seam so unit tests can assert without the Firebase SDK.
 */
interface AnalyticsBackend {
    fun logEvent(name: String, params: Map<String, Any?>)
    fun setUserProperty(name: String, value: String?)
}
