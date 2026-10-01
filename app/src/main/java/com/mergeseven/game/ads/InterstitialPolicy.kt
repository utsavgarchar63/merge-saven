package com.mergeseven.game.ads
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.liveops.LiveOpsGates
import javax.inject.Inject
import javax.inject.Singleton

data class InterstitialDecision(val allow: Boolean, val reason: String)
interface InterstitialPolicy {
    suspend fun evaluate(sessionWon: Boolean, inTutorial: Boolean, suppressAds: Boolean): InterstitialDecision
    suspend fun onSessionEnded() {}
    suspend fun onInterstitialShown() {}
    suspend fun finishSession(id: String, campaignWon: Boolean) {}
    suspend fun addPlaytime(ms: Long) {}
}
@Singleton
class DefaultInterstitialPolicy @Inject constructor(
    private val liveConfig: LiveConfig, private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates, private val exposure: AdExposureStore
) : InterstitialPolicy {
    override suspend fun evaluate(sessionWon: Boolean, inTutorial: Boolean, suppressAds: Boolean): InterstitialDecision {
        if (!featureFlags.isEnabled(Feature.AF9) || !liveOpsGates.adsAllowed()) return InterstitialDecision(false, "ads_off")
        if (!sessionWon) return InterstitialDecision(false, "after_loss")
        if (inTutorial) return InterstitialDecision(false, "tutorial")
        if (suppressAds) return InterstitialDecision(false, "mode_suppress")
        return decideInterstitial(exposure.snapshot(), liveConfig.adPolicy(), System.currentTimeMillis())
    }
    override suspend fun finishSession(id: String, campaignWon: Boolean) = exposure.finishSession(id, campaignWon)
    override suspend fun addPlaytime(ms: Long) = exposure.addPlaytime(ms)
}
class FakeInterstitialPolicy(var allow: Boolean = false, var reason: String = "fake") : InterstitialPolicy {
    var sessionEndedCount = 0
    var shownCount = 0
    override suspend fun evaluate(sessionWon: Boolean, inTutorial: Boolean, suppressAds: Boolean): InterstitialDecision = when {
        !sessionWon -> InterstitialDecision(false, "after_loss")
        inTutorial -> InterstitialDecision(false, "tutorial")
        suppressAds -> InterstitialDecision(false, "mode_suppress")
        else -> InterstitialDecision(allow, reason)
    }
    override suspend fun onSessionEnded() { sessionEndedCount++ }
    override suspend fun onInterstitialShown() { shownCount++ }
}
