package com.mergeseven.game.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mergeseven.game.billing.EntitlementStore
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.liveops.LiveOpsGates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class InterstitialDecision(
    val allow: Boolean,
    val reason: String
)

interface InterstitialPolicy {
    suspend fun evaluate(
        sessionWon: Boolean,
        inTutorial: Boolean,
        suppressAds: Boolean
    ): InterstitialDecision

    suspend fun onSessionEnded()
    suspend fun onInterstitialShown()
}

private val Context.interstitialStore: DataStore<Preferences> by preferencesDataStore("interstitial_policy")

/**
 * RC-driven interstitial rules (AF9-03): frequency, never after loss, never during tutorial.
 */
@Singleton
class DefaultInterstitialPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val liveConfig: LiveConfig,
    private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val entitlementStore: EntitlementStore
) : InterstitialPolicy {

    private val sessionsKey = intPreferencesKey("sessions_since_ad")
    private val lastShownKey = longPreferencesKey("last_shown_ms")

    override suspend fun evaluate(
        sessionWon: Boolean,
        inTutorial: Boolean,
        suppressAds: Boolean
    ): InterstitialDecision {
        if (!featureFlags.isEnabled(Feature.AF9)) return InterstitialDecision(false, "af9_off")
        if (!liveOpsGates.adsAllowed()) return InterstitialDecision(false, "kill_ads")
        if (entitlementStore.removeAdsOrPremium.value) return InterstitialDecision(false, "remove_ads")
        if (suppressAds) return InterstitialDecision(false, "mode_suppress")
        if (inTutorial) return InterstitialDecision(false, "tutorial")
        if (!sessionWon) return InterstitialDecision(false, "after_loss")
        val freq = liveConfig.adFrequency().coerceAtLeast(1)
        val sessions = context.interstitialStore.data.first()[sessionsKey] ?: 0
        if (sessions < freq) return InterstitialDecision(false, "frequency_$sessions/$freq")
        return InterstitialDecision(true, "ok")
    }

    override suspend fun onSessionEnded() {
        context.interstitialStore.edit { prefs ->
            val cur = prefs[sessionsKey] ?: 0
            prefs[sessionsKey] = cur + 1
        }
    }

    override suspend fun onInterstitialShown() {
        context.interstitialStore.edit { prefs ->
            prefs[sessionsKey] = 0
            prefs[lastShownKey] = System.currentTimeMillis()
        }
    }
}

/** In-memory policy for unit tests. */
class FakeInterstitialPolicy(
    var allow: Boolean = false,
    var reason: String = "fake"
) : InterstitialPolicy {
    var sessionEndedCount: Int = 0
    var shownCount: Int = 0

    override suspend fun evaluate(
        sessionWon: Boolean,
        inTutorial: Boolean,
        suppressAds: Boolean
    ): InterstitialDecision {
        if (!sessionWon) return InterstitialDecision(false, "after_loss")
        if (inTutorial) return InterstitialDecision(false, "tutorial")
        if (suppressAds) return InterstitialDecision(false, "mode_suppress")
        return InterstitialDecision(allow, reason)
    }

    override suspend fun onSessionEnded() {
        sessionEndedCount++
    }

    override suspend fun onInterstitialShown() {
        shownCount++
    }
}
