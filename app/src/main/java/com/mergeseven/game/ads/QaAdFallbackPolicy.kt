package com.mergeseven.game.ads

/** Demo inventory is a development aid, never a revenue fallback for release players. */
internal object QaAdFallbackPolicy {
    fun allows(debug: Boolean, configured: Boolean, production: Boolean, testDevice: Boolean,
               alreadyFallback: Boolean): Boolean =
        debug && configured && production && testDevice && !alreadyFallback
}
