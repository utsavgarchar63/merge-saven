package com.mergeseven.game.game.objectives

import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.TileTrait

/**
 * Single source of truth for objective progress / win / fail (AF1-13/14).
 */
object ObjectiveEvaluator {

    fun effectiveObjectives(state: GameState): List<LevelObjective> {
        if (state.objectives.isNotEmpty()) return state.objectives
        return listOf(
            LevelObjective.ReachValue(id = "reach_target", value = state.targetValue)
        )
    }

    fun progress(state: GameState, objective: LevelObjective): ObjectiveProgress = when (objective) {
        is LevelObjective.ReachValue -> {
            val maxTile = state.board.activeTiles().maxOfOrNull { it.value } ?: 0
            ObjectiveProgress(
                id = objective.id,
                label = "Reach ${objective.value}",
                current = maxTile.toLong(),
                target = objective.value.toLong(),
                isComplete = maxTile >= objective.value
            )
        }

        is LevelObjective.ClearAllStone -> {
            val stones = state.board.activeTiles().count { it.trait == TileTrait.STONE }
            ObjectiveProgress(
                id = objective.id,
                label = "Clear stone",
                current = if (stones == 0) 1L else 0L,
                target = 1L,
                isComplete = stones == 0
            )
        }

        is LevelObjective.SurviveMoves -> {
            val failed = state.isGameOver && state.moves < objective.moves
            ObjectiveProgress(
                id = objective.id,
                label = "Survive ${objective.moves}",
                current = state.moves.toLong().coerceAtMost(objective.moves.toLong()),
                target = objective.moves.toLong(),
                isComplete = !failed && state.moves >= objective.moves,
                isFailed = failed
            )
        }

        is LevelObjective.ScoreInMoves -> {
            val complete = state.score >= objective.score && state.moves <= objective.moves
            val failed = state.moves > objective.moves && state.score < objective.score
            ObjectiveProgress(
                id = objective.id,
                label = "Score ${objective.score}",
                current = state.score.coerceAtMost(objective.score),
                target = objective.score,
                isComplete = complete,
                isFailed = failed
            )
        }

        is LevelObjective.CollectValue -> {
            val collected = state.collectedByValue[objective.tileValue] ?: 0
            ObjectiveProgress(
                id = objective.id,
                label = "Collect ${objective.count}×${objective.tileValue}",
                current = collected.toLong().coerceAtMost(objective.count.toLong()),
                target = objective.count.toLong(),
                isComplete = collected >= objective.count
            )
        }
    }

    fun allProgress(state: GameState): List<ObjectiveProgress> =
        effectiveObjectives(state).map { progress(state, it) }

    fun allComplete(state: GameState): Boolean {
        val objectives = effectiveObjectives(state)
        if (objectives.isEmpty()) return false
        return objectives.all { progress(state, it).isComplete }
    }

    fun anyFailed(state: GameState): Boolean =
        effectiveObjectives(state).any { progress(state, it).isFailed }
}
