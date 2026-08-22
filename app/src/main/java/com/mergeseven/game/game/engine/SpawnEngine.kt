package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.*
import kotlin.random.Random

/**
 * Generates pieces and tile values using weighted randomization.
 * See Master Plan Section 18 (Spawn / Random System), Section 92 (Piece Pool).
 */
class SpawnEngine(
    private val random: Random = Random.Default
) {

    private var nextPieceId: Long = System.nanoTime()

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

    /**
     * Generate a random piece appropriate for the current level.
     */
    fun generatePiece(level: Int): TilePiece {
        val shape = selectShape(level)
        val cells = shape.map { offset ->
            PieceCell(
                offset = offset,
                value = generateValue(level)
            )
        }

        return TilePiece(
            id = nextPieceId++,
            cells = cells,
            rotation = 0
        )
    }

    /**
     * Generate a random tile value using weighted distribution.
     * See Master Plan Section 18.
     */
    fun generateValue(level: Int = 1): Int {
        val weights = Constants.SPAWN_WEIGHTS
        val totalWeight = weights.values.sum()
        var roll = random.nextInt(totalWeight)

        for ((value, weight) in weights) {
            roll -= weight
            if (roll < 0) return value
        }

        // Fallback
        return weights.keys.first()
    }

    /**
     * Select a piece shape. Start with simpler shapes at lower levels.
     */
    private fun selectShape(level: Int): List<HexCoord> {
        // At early levels, prefer single and pair shapes
        val maxShapeIndex = when {
            level <= 2 -> 3    // Single + pairs only
            level <= 5 -> 4    // Add diagonal pair
            else -> pieceShapes.size - 1  // All shapes
        }

        val index = random.nextInt(maxShapeIndex + 1)
        return pieceShapes[index]
    }
}
