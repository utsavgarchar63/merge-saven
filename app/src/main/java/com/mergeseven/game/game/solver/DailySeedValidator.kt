package com.mergeseven.game.game.solver

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.modes.DailyMode
import com.mergeseven.game.game.modes.ModeEndReason
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import com.mergeseven.game.game.modes.ModeSessionContext
import com.mergeseven.game.game.objectives.ObjectiveEvaluator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline daily seed filter — bump xor salt until a cheap greedy playout looks solvable (AF4-06).
 */
@Singleton
class DailySeedValidator @Inject constructor(
    private val gameEngine: GameEngine,
    private val moveSolver: MoveSolver
) {

    fun resolveSeed(
        dateIso: String,
        targetScore: Int = DailyMode.DEFAULT_TARGET,
        maxMoves: Int = DailyMode.MAX_MOVES
    ): Long {
        for (salt in 0..Constants.DAILY_SEED_MAX_SALT) {
            val seed = ModeSeeds.dailySeed(dateIso) xor salt.toLong()
            if (isPromising(seed, dateIso, targetScore, maxMoves)) {
                return seed
            }
        }
        return ModeSeeds.dailySeed(dateIso)
    }

    fun isPromising(
        seed: Long,
        dateIso: String,
        targetScore: Int,
        maxMoves: Int
    ): Boolean {
        val budget = HintSearchBudget(Constants.DAILY_SEED_VALIDATE_BUDGET_MS)
        val daily = DailyMode()
        var state = daily.createSession(
            ModeSessionContext(
                levelId = 1,
                seed = seed,
                bestScore = 0L,
                dateKey = dateIso,
                targetScore = targetScore,
                gameEngine = gameEngine
            )
        )
        var moves = 0
        while (moves < maxMoves && budget.hasTime()) {
            if (ObjectiveEvaluator.allComplete(state) || state.score >= targetScore) {
                return true
            }
            if (state.isGameOver || gameEngine.isGameOver(state)) {
                return false
            }
            val hint = moveSolver.findBestMove(
                state = state,
                profile = DifficultyProfiles.STANDARD,
                budget = HintSearchBudget(5L)
            ) ?: moveSolver.findAnyLegalMove(state) ?: return false

            val piece = state.trayPieces[hint.slotIndex]!!.copy(rotation = hint.rotation)
            val result = gameEngine.placePiece(state, piece, hint.origin, hint.slotIndex)
            state = result.state.copy(previousState = null, modeId = ModeIds.DAILY)
            val outcome = daily.afterMove(result.state.copy(previousState = null), result, gameEngine)
            state = outcome.state.copy(previousState = null)
            if (outcome.endReason != null) {
                return outcome.endReason == ModeEndReason.WON || state.score >= targetScore
            }
            moves++
        }
        return state.score >= (targetScore * 0.35).toLong()
    }
}
