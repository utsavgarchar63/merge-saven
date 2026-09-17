package com.mergeseven.game.ads

/**
 * Mediation-ready ad placements (AF9). Unit IDs stay outside call sites.
 */
enum class AdPlacement {
    CONTINUE,
    FUNDS_COINS,
    FREE_HINT,
    EXTRA_DAILY,
    DOUBLE_COINS,
    INTERSTITIAL,
    BANNER
}

sealed class AdResult {
    data object Rewarded : AdResult()
    data object Completed : AdResult()
    data object Dismissed : AdResult()
    data object NoFill : AdResult()
    data class Failed(val message: String) : AdResult()
}

/**
 * App-facing ad API. Implementations must not load before consent resolves.
 */
interface AdService {
    fun isReady(placement: AdPlacement): Boolean
    fun preload(placement: AdPlacement)
    suspend fun showRewarded(activity: android.app.Activity, placement: AdPlacement): AdResult
    suspend fun showInterstitial(activity: android.app.Activity): AdResult
    fun bindBanner(activity: android.app.Activity, container: android.view.ViewGroup)
    fun unbindBanner()
    fun destroy()
}
