package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants

/**
 * Calculates scores for merges and chains.
 * See Master Plan Section 20.
 *
 * Base formula: mergeScore = mergedValue × mergedTileCount
 * Chain multiplier applied for successive merges.
 */
class ScoreEngine {

    /**
     * Calculate score earned from a single merge.
     *
     * @param mergedValue The resulting tile value after merge.
     * @param tileCount The number of tiles that were merged.
     * @param chainIndex The chain step (0 = first merge, 1 = second, etc.).
     * @return The score earned.
     */
    fun calculateMergeScore(
        mergedValue: Int,
        tileCount: Int,
        chainIndex: Int = 0
    ): Long {
        val baseScore = mergedValue.toLong() * tileCount.toLong()
        val multiplier = getChainMultiplier(chainIndex)
        return (baseScore * multiplier).toLong()
    }

    /**
     * Get the chain multiplier for a given chain index.
     * See Master Plan Section 20.
     */
    fun getChainMultiplier(chainIndex: Int): Float {
        val multipliers = Constants.CHAIN_MULTIPLIERS
        return if (chainIndex < multipliers.size) {
            multipliers[chainIndex]
        } else {
            multipliers.last()
        }
    }

    /**
     * Calculate coins earned from a merge.
     * More tiles and higher values earn more coins.
     */
    fun calculateMergeCoins(
        mergedValue: Int,
        tileCount: Int,
        chainIndex: Int = 0
    ): Int {
        val base = when {
            tileCount >= 5 -> 3
            tileCount >= 4 -> 2
            else -> 1
        }
        // Bonus for chain reactions
        val chainBonus = if (chainIndex > 0) chainIndex else 0
        return base + chainBonus
    }
}
