package com.mergeseven.game.game.solver

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.TilePiece
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Depth-limited placement search over the current tray (AF4-02).
 */
@Singleton
class MoveSolver @Inject constructor(
    private val gameEngine: GameEngine,
    private val placementEngine: PlacementEngine,
    private val evaluator: BoardEvaluator
) {

    fun findBestMove(
        state: GameState,
        profile: DifficultyProfile = DifficultyProfiles.STANDARD,
        budget: HintSearchBudget = HintSearchBudget(Constants.HINT_SOLVER_BUDGET_MS)
    ): MoveHint? {
        val depth = profile.solverDepth.coerceIn(1, 2)
        val candidates = enumerateCandidates(state, budget)
        if (candidates.isEmpty()) return null

        var best: MoveHint? = null
        for (candidate in candidates) {
            if (budget.expired()) break
            val after = simulate(state, candidate) ?: continue
            var score = evaluator.score(after, profile.evaluatorWeights)
            if (depth >= 2 && budget.hasTime()) {
                score += peekDepthTwo(after, profile, budget) * 0.35
            }
            val hint = candidate.copy(score = score)
            if (best == null || hint.score > best.score) {
                best = hint
            }
        }
        return best
    }

    /** Any legal move — used when the budget expires mid-search. */
    fun findAnyLegalMove(state: GameState): MoveHint? {
        state.trayPieces.forEachIndexed { slot, piece ->
            if (piece == null) return@forEachIndexed
            for (rotation in 0 until Constants.ROTATION_STEPS) {
                val rotated = piece.copy(rotation = rotation)
                val origins = placementEngine.findValidOrigins(state.board, rotated)
                val origin = origins.firstOrNull() ?: continue
                return MoveHint(slot, origin, rotation, score = 0.0)
            }
        }
        return null
    }

    private fun enumerateCandidates(
        state: GameState,
        budget: HintSearchBudget
    ): List<MoveHint> {
        val out = ArrayList<MoveHint>()
        state.trayPieces.forEachIndexed { slot, piece ->
            if (piece == null) return@forEachIndexed
            for (rotation in 0 until Constants.ROTATION_STEPS) {
                if (budget.expired()) return out
                val rotated = piece.copy(rotation = rotation)
                for (origin in placementEngine.findValidOrigins(state.board, rotated)) {
                    if (budget.expired()) return out
                    out.add(MoveHint(slot, origin, rotation, score = 0.0))
                }
            }
        }
        return out
    }

    private fun simulate(state: GameState, hint: MoveHint): GameState? {
        val piece = state.trayPieces.getOrNull(hint.slotIndex) ?: return null
        val rotated: TilePiece = piece.copy(rotation = hint.rotation)
        if (!gameEngine.canPlace(state, rotated, hint.origin)) return null
        val result = gameEngine.placePiece(state, rotated, hint.origin, hint.slotIndex)
        return result.state.copy(previousState = null)
    }

    private fun peekDepthTwo(
        state: GameState,
        profile: DifficultyProfile,
        budget: HintSearchBudget
    ): Double {
        val next = enumerateCandidates(state, budget).take(TOP_K)
        var best = Double.NEGATIVE_INFINITY
        for (candidate in next) {
            if (budget.expired()) break
            val after = simulate(state, candidate) ?: continue
            val score = evaluator.score(after, profile.evaluatorWeights)
            if (score > best) best = score
        }
        return if (best == Double.NEGATIVE_INFINITY) 0.0 else best
    }

    private companion object {
        const val TOP_K = 8
    }
}
