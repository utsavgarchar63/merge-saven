package com.mergeseven.game.ads

import android.app.Activity
import android.view.ViewGroup

/** Controllable [AdService] for JVM tests. */
class FakeAdService(
    var rewardedResult: AdResult = AdResult.Rewarded,
    var interstitialResult: AdResult = AdResult.Completed,
    var ready: Boolean = true
) : AdService {
    val preloadCalls = mutableListOf<AdPlacement>()
    val showRewardedCalls = mutableListOf<AdPlacement>()
    var showInterstitialCalls: Int = 0
    var bannerBindCalls: Int = 0

    override fun isReady(placement: AdPlacement): Boolean = ready

    override fun preload(placement: AdPlacement) {
        preloadCalls += placement
    }

    override suspend fun showRewarded(activity: Activity, placement: AdPlacement): AdResult {
        showRewardedCalls += placement
        return rewardedResult
    }

    override suspend fun showInterstitial(activity: Activity): AdResult {
        showInterstitialCalls++
        return interstitialResult
    }

    override fun bindBanner(activity: Activity, container: ViewGroup) {
        bannerBindCalls++
    }

    override fun unbindBanner() = Unit

    override fun destroy() = Unit
}
