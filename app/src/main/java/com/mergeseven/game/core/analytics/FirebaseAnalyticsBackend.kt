package com.mergeseven.game.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsBackend @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsBackend {

    override fun logEvent(name: String, params: Map<String, Any?>) {
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
