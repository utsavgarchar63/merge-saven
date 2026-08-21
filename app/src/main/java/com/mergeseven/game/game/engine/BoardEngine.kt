package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.*

/**
 * Responsible for creating and managing the hex board.
 * See Master Plan Section 54 (Phase 4).
 */
class BoardEngine {

    /**
     * Creates a hexagonal board with the default radius.
     * Uses axial coordinates to generate a hex-shaped playable area.
     *
     * For a hex board with radius N, the valid cells satisfy:
     * max(|q|, |r|, |s|) <= N, where s = -q - r
     */
    fun createBoard(radius: Int = Constants.DEFAULT_BOARD_RADIUS): BoardState {
        val playableCells = mutableSetOf<HexCoord>()
        val cells = mutableMapOf<HexCoord, Tile?>()

        for (q in -radius..radius) {
            val r1 = maxOf(-radius, -q - radius)
            val r2 = minOf(radius, -q + radius)
            for (r in r1..r2) {
                val coord = HexCoord(q, r)
                playableCells.add(coord)
                cells[coord] = null
            }
        }

        return BoardState(
            cells = cells,
            playableCells = playableCells
        )
    }

    /**
     * Creates a board with specific blocked cells (for level variety).
     * See Master Plan Section 25.
     */
    fun createBoard(
        radius: Int = Constants.DEFAULT_BOARD_RADIUS,
        blockedCells: Set<HexCoord> = emptySet()
    ): BoardState {
        val base = createBoard(radius)
        val playable = base.playableCells - blockedCells
        val cells = base.cells.filterKeys { it in playable }
        return BoardState(
            cells = cells,
            playableCells = playable
        )
    }

    /**
     * Returns the number of playable cells for a given radius.
     * Formula: 3 * radius * (radius + 1) + 1
     */
    fun cellCount(radius: Int): Int = 3 * radius * (radius + 1) + 1
}
