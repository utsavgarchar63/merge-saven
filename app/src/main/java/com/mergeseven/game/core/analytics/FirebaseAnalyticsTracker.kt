package com.mergeseven.game.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production analytics tracker (AF6-02). Call sites stay on [AnalyticsTracker].
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val backend: AnalyticsBackend
) : AnalyticsTracker {

    override fun logEvent(name: String, params: Map<String, Any?>) {
        runCatching { backend.logEvent(name, params) }
    }

    override fun setUserProperty(name: String, value: String?) {
        runCatching { backend.setUserProperty(name, value) }
    }
}
