package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.*

/**
 * Determines when the game is over.
 * See Master Plan Section 19, Phase 11.
 *
 * Game over means NO piece from the queue can be placed
 * in ANY valid origin with ANY allowed rotation.
 *
 * Do NOT only check whether the board is visually full.
 */
class GameOverEngine(
    private val placementEngine: PlacementEngine
) {

    /**
     * Check if the game is over.
     * Algorithm from Section 19:
     *
     * For every candidate piece
     *   For every playable origin
     *     For every allowed rotation
     *       canPlace? → YES → not game over
     * If none work → GAME OVER
     */
    fun isGameOver(state: GameState): Boolean {
        val candidatePieces = state.trayPieces.filterNotNull()
        if (candidatePieces.isEmpty()) return false

        for (piece in candidatePieces) {
            for (rotation in 0 until Constants.ROTATION_STEPS) {
                val rotatedPiece = if (rotation == 0) piece
                    else piece.copy(rotation = rotation)

                for (origin in state.board.playableCells) {
                    if (placementEngine.canPlace(state.board, rotatedPiece, origin)) {
                        return false // Found a valid placement → not game over
                    }
                }
            }
        }

        return true // No valid placement found → game over
    }
}
