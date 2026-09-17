package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.*

/**
 * Generates pieces and tile values using weighted randomization.
 * See Master Plan Section 18 (Spawn / Random System), Section 92 (Piece Pool).
 *
 * Holds no random state of its own. The caller passes a [GameRandom] positioned at the run's
 * current point in the sequence, so the same run always produces the same pieces.
 */
class SpawnEngine {

    /**
     * Predefined piece shapes.
     * See Master Plan Section 92.
     */
    private val pieceShapes: List<List<HexCoord>> = listOf(
        // Single tile
        listOf(HexCoord(0, 0)),

        // Vertical pair (along r axis)
        listOf(HexCoord(0, 0), HexCoord(0, 1)),

        // Horizontal pair (along q axis)
        listOf(HexCoord(0, 0), HexCoord(1, 0)),

        // Diagonal pair
        listOf(HexCoord(0, 0), HexCoord(1, -1)),

        // Triangle (3 cells)
        listOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1)),

        // Line of 3
        listOf(HexCoord(0, 0), HexCoord(0, 1), HexCoord(0, 2))
    )

    private val tripleShapes: List<List<HexCoord>> = listOf(
        listOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1)),
        listOf(HexCoord(0, 0), HexCoord(0, 1), HexCoord(0, 2)),
        listOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(2, 0)),
        listOf(HexCoord(0, 0), HexCoord(1, -1), HexCoord(1, 0))
    )

    /**
     * Generate a random piece appropriate for the current level / mode hints.
     */
    fun generatePiece(
        level: Int,
        random: GameRandom,
        hints: SpawnHints = SpawnHints()
    ): TilePiece {
        val shape = if (hints.forceTriple) {
            tripleShapes[random.nextInt(tripleShapes.size)]
        } else {
            selectShape(level, random)
        }
        val cells = shape.map { offset ->
            PieceCell(
                offset = offset,
                value = generateValue(level, random, hints)
            )
        }

        return TilePiece(
            id = random.nextId(),
            cells = cells,
            rotation = 0
        )
    }

    /**
     * Generate a random tile value using weighted distribution.
     * Higher [SpawnHints.tier] shifts weight toward larger values (Endless).
     */
    fun generateValue(
        level: Int = 1,
        random: GameRandom,
        hints: SpawnHints = SpawnHints()
    ): Int {
        val weights = weightsForTier(hints.tier, hints.baseWeights).toMutableMap()
        for ((value, boost) in hints.boardValueBoost) {
            if (boost <= 0) continue
            weights[value] = (weights[value] ?: 0) + boost
        }
        val filtered = weights.filterValues { it > 0 }
        val totalWeight = filtered.values.sum()
        if (totalWeight <= 0) return 2
        var roll = random.nextInt(totalWeight)

        for ((value, weight) in filtered) {
            roll -= weight
            if (roll < 0) return value
        }

        return filtered.keys.first()
    }

    fun weightsForTier(tier: Int, baseWeights: Map<Int, Int>? = null): Map<Int, Int> {
        val base = (baseWeights ?: Constants.SPAWN_WEIGHTS).toMutableMap()
        if (tier <= 0) return base
        // Each tier: shift a few points from 2/4 toward 8/16/32.
        repeat(tier.coerceAtMost(8)) {
            fun shift(from: Int, to: Int, amount: Int) {
                val available = base[from] ?: return
                val moved = amount.coerceAtMost(available)
                if (moved <= 0) return
                base[from] = available - moved
                base[to] = (base[to] ?: 0) + moved
            }
            shift(2, 8, 2)
            shift(4, 16, 2)
            shift(8, 32, 1)
        }
        return base.filterValues { it > 0 }
    }

    private fun selectShape(level: Int, random: GameRandom): List<HexCoord> {
        val maxShapeIndex = when {
            level <= 2 -> 3
            level <= 5 -> 4
            else -> pieceShapes.size - 1
        }

        val index = random.nextInt(maxShapeIndex + 1)
        return pieceShapes[index]
    }
}
