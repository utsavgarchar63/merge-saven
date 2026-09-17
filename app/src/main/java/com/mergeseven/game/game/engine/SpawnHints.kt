package com.mergeseven.game.game.engine

/**
 * Spawn modifiers supplied by the active [com.mergeseven.game.game.modes.GameMode] (AF2),
 * optional adaptive board boost (AF4-07), and Remote Config base weights (AF6-04).
 */
data class SpawnHints(
    val tier: Int = 0,
    val forceTriple: Boolean = false,
    /** Extra weight added to specific tile values when generating. */
    val boardValueBoost: Map<Int, Int> = emptyMap(),
    /** When non-null, replaces [com.mergeseven.game.core.Constants.SPAWN_WEIGHTS] as the base map. */
    val baseWeights: Map<Int, Int>? = null
)
