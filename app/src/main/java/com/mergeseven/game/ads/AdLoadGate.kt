package com.mergeseven.game.ads

/** All rewarded offers use one unit and one SDK inventory slot. */
internal fun inventoryKey(placement: AdPlacement): AdPlacement = when (placement) {
    AdPlacement.BANNER, AdPlacement.INTERSTITIAL -> placement
    else -> AdPlacement.FUNDS_COINS
}
internal val rewardedPlacements = AdPlacement.entries.filter {
    it != AdPlacement.BANNER && it != AdPlacement.INTERSTITIAL
}.toSet()

/** Deduplicates format loads and shares failure backoff across rewarded buttons. */
internal class AdLoadGate(private val retryMs: Long = 30_000L) {
    private val loading = mutableSetOf<AdPlacement>()
    private val attempts = mutableMapOf<AdPlacement, Long>()
    @Synchronized fun tryBegin(placement: AdPlacement, now: Long): Boolean {
        val key = inventoryKey(placement)
        if (key in loading || attempts[key]?.let { now - it < retryMs } == true) return false
        loading += key
        attempts[key] = now
        return true
    }
    @Synchronized fun finished(placement: AdPlacement) { loading -= inventoryKey(placement) }
    @Synchronized fun allowRetry(placement: AdPlacement) { attempts.remove(inventoryKey(placement)) }
    @Synchronized fun clear() { loading.clear(); attempts.clear() }
}
