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
    private val retryAt = mutableMapOf<AdPlacement, Long>()
    private val failures = mutableMapOf<AdPlacement, Int>()
    @Synchronized fun tryBegin(placement: AdPlacement, now: Long): Boolean {
        val key = inventoryKey(placement)
        if (key in loading || now < (retryAt[key] ?: 0L) ||
            attempts[key]?.let { now - it < retryMs } == true) return false
        loading += key
        attempts[key] = now
        return true
    }
    @Synchronized fun finished(placement: AdPlacement) { loading -= inventoryKey(placement) }
    /** Keep recovering after no-fill/offline errors without retrying on every button tap. */
    @Synchronized fun failed(placement: AdPlacement, now: Long): Long {
        val key = inventoryKey(placement)
        loading -= key
        val count = ((failures[key] ?: 0) + 1).coerceAtMost(5)
        failures[key] = count
        val delay = (retryMs * (1L shl (count - 1))).coerceAtMost(300_000L)
        retryAt[key] = now + delay
        return delay
    }
    @Synchronized fun allowRetry(placement: AdPlacement) {
        val key = inventoryKey(placement)
        attempts.remove(key); retryAt.remove(key); failures.remove(key)
    }
    @Synchronized fun clear() { loading.clear(); attempts.clear(); retryAt.clear(); failures.clear() }
}
