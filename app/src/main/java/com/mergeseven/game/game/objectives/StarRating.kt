package com.mergeseven.game.game.objectives

import com.mergeseven.game.game.model.GameState

/**
 * Efficiency-based 1–3 star rating once all objectives are complete (AF1-15).
 */
object StarRating {

    const val DEFAULT_THREE_STAR_MOVES = 25
    const val DEFAULT_TWO_STAR_MOVES = 40

    fun compute(
        state: GameState,
        threeStarMoveCap: Int = state.threeStarMoveCap,
        twoStarMoveCap: Int = state.twoStarMoveCap
    ): Int {
        if (!ObjectiveEvaluator.allComplete(state)) return 0

        val three = threeStarMoveCap.coerceAtLeast(1)
        val two = twoStarMoveCap.coerceAtLeast(three)

        return when {
            state.moves <= three -> 3
            state.moves <= two -> 2
            else -> 1
        }
    }
}
