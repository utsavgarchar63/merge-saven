package com.mergeseven.game.core.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope

/**
 * Local feature-flag surface. Release builds hard-return false for every flag so unfinished
 * phases never light up by accident.
 */
interface FeatureFlags {
    fun isEnabled(feature: Feature): Boolean

    fun observe(feature: Feature): Flow<Boolean>

    /** Ignored in release. In debug, persists the override for the debug menu. */
    suspend fun setEnabled(feature: Feature, enabled: Boolean)

    fun snapshot(): Map<Feature, Boolean>
}

/**
 * Convenience for ViewModels / Compose: a hot [StateFlow] for a single phase flag.
 */
fun FeatureFlags.enabledState(
    feature: Feature,
    scope: CoroutineScope
): StateFlow<Boolean> =
    observe(feature).stateIn(
        scope = scope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = isEnabled(feature)
    )

/** True when [feature] is on; use at AF entry points instead of inventing ad-hoc checks. */
fun FeatureFlags.requireEnabled(feature: Feature): Boolean = isEnabled(feature)
