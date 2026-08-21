package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.*

/**
 * Validates and executes piece placements on the board.
 * See Master Plan Section 15 (Placement Rules), Phase 7.
 */
class PlacementEngine {

    private var nextTileId: Long = System.nanoTime()

    /**
     * Check if a piece can be placed at the given origin.
     *
     * Returns false if:
     * - Any piece cell maps to a position outside the board
     * - Any target cell is already occupied
     * - Any target cell is not in the playable area
     */
    fun canPlace(
        board: BoardState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean {
        val absoluteCells = piece.absoluteCells(origin)
        return absoluteCells.all { (coord, _) ->
            board.isPlayable(coord) && board.isEmpty(coord)
        }
    }

    /**
     * Place a piece on the board and return the list of created tiles.
     * Does NOT validate — call canPlace() first.
     *
     * @return The list of tiles that were placed on the board.
     */
    fun place(
        board: BoardState,
        piece: TilePiece,
        origin: HexCoord
    ): List<Tile> {
        return piece.absoluteCells(origin).map { (coord, value) ->
            Tile(
                id = nextTileId++,
                value = value,
                cell = coord
            )
        }
    }

    /**
     * Find all valid placement origins for a piece on the board.
     * Used by the game-over engine to check all possibilities.
     */
    fun findValidOrigins(
        board: BoardState,
        piece: TilePiece
    ): List<HexCoord> {
        return board.playableCells.filter { origin ->
            canPlace(board, piece, origin)
        }.toList()
    }
}
