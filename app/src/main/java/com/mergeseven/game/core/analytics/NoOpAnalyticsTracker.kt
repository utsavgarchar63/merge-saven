package com.mergeseven.game.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/** Default AF0 implementation — never throws, never talks to the network. */
@Singleton
class NoOpAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit

    override fun setUserProperty(name: String, value: String?) = Unit
}
