package com.mergeseven.game.ads

/**
 * Mediation-ready ad placements (AF9). Unit IDs stay outside call sites.
 */
enum class AdPlacement {
    CONTINUE,
    FUNDS_COINS,
    INSUFFICIENT_COINS,
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
    val loadStates: kotlinx.coroutines.flow.StateFlow<Map<AdPlacement, AdLoadState>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    val availability: kotlinx.coroutines.flow.StateFlow<Set<AdPlacement>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptySet())
    val fullscreenShowing: kotlinx.coroutines.flow.StateFlow<Boolean>
        get() = kotlinx.coroutines.flow.MutableStateFlow(false)
    fun isReady(placement: AdPlacement): Boolean
    fun preload(placement: AdPlacement)
    /** Explicit recovery request. A late load never opens an ad automatically. */
    fun requestAd(activity: android.app.Activity, placement: AdPlacement) { preload(placement) }
    suspend fun showRewarded(activity: android.app.Activity, placement: AdPlacement, onEarned: suspend () -> Unit = {}): AdResult
    suspend fun showInterstitial(activity: android.app.Activity): AdResult
    fun bindBanner(activity: android.app.Activity, container: android.view.ViewGroup)
    fun pauseBanner(container: android.view.ViewGroup? = null) {}
    fun resumeBanner(container: android.view.ViewGroup? = null) {}
    fun unbindBanner(container: android.view.ViewGroup? = null)
    fun destroy()
}
