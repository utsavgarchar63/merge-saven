package com.mergeseven.game.ads

import javax.inject.Inject
import javax.inject.Singleton

/** Warms high-value rewarded placements (AF9-04). */
@Singleton
class AdPreloader @Inject constructor(
    private val adService: AdService
) {
    fun warmGameOver() {
        adService.preload(AdPlacement.CONTINUE)
        adService.preload(AdPlacement.FUNDS_COINS)
        adService.preload(AdPlacement.DOUBLE_COINS)
    }

    fun warmShop() {
        adService.preload(AdPlacement.FUNDS_COINS)
    }

    fun warmDaily() {
        adService.preload(AdPlacement.EXTRA_DAILY)
    }

    fun warmHint() {
        adService.preload(AdPlacement.FREE_HINT)
    }

    fun warmInterstitial() {
        adService.preload(AdPlacement.INTERSTITIAL)
    }
}
