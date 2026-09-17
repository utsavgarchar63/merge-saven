package com.mergeseven.game.game.solver

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soft warning when the board is nearly unplayable (AF4-05).
 */
@Singleton
class DeadlockPredictor @Inject constructor(
    private val placementEngine: PlacementEngine,
    private val gameOverEngine: GameOverEngine
) {

    fun isNearDeadlock(state: GameState): Boolean {
        if (state.isGameOver) return false
        if (gameOverEngine.isGameOver(state)) return false
        val minLegal = minLegalPlacements(state)
        return minLegal in 0..1
    }

    fun minLegalPlacements(state: GameState): Int {
        val pieces = state.trayPieces.filterNotNull()
        if (pieces.isEmpty()) return Int.MAX_VALUE
        var min = Int.MAX_VALUE
        for (piece in pieces) {
            val origins = LinkedHashSet<com.mergeseven.game.game.model.HexCoord>()
            for (rotation in 0 until Constants.ROTATION_STEPS) {
                val rotated = piece.copy(rotation = rotation)
                origins.addAll(placementEngine.findValidOrigins(state.board, rotated))
            }
            if (origins.size < min) min = origins.size
        }
        return if (min == Int.MAX_VALUE) 0 else min
    }
}
