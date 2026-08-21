package com.mergeseven.game.game.rules

import com.mergeseven.game.core.Constants

/**
 * Spawn rule configuration.
 * See Master Plan Section 18.
 *
 * Controls how pieces and tile values are generated.
 */
data class SpawnRule(
    /** Weighted distribution of tile values. */
    val valueWeights: Map<Int, Int> = Constants.SPAWN_WEIGHTS,

    /** Maximum number of cells in a single piece. */
    val maxPieceCells: Int = 3,

    /** Whether to consider board fill ratio when spawning. */
    val adaptToFillRatio: Boolean = false
)
