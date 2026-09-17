package com.mergeseven.game.ads

import android.app.Activity
import android.view.ViewGroup

/** Default when AF9 is off or ads are killed — never requests inventory. */
class NoOpAdService : AdService {
    var loadAttempts: Int = 0
        private set

    override fun isReady(placement: AdPlacement): Boolean = false

    override fun preload(placement: AdPlacement) {
        loadAttempts++
    }

    override suspend fun showRewarded(activity: Activity, placement: AdPlacement): AdResult =
        AdResult.NoFill

    override suspend fun showInterstitial(activity: Activity): AdResult = AdResult.NoFill

    override fun bindBanner(activity: Activity, container: ViewGroup) = Unit

    override fun unbindBanner() = Unit

    override fun destroy() = Unit
}
