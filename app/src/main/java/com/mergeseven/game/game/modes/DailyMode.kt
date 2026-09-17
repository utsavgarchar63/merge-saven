package com.mergeseven.game.game.modes

import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.objectives.LevelObjective
import com.mergeseven.game.game.objectives.ObjectiveEvaluator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.DAILY
    override val saveSlotId: String = ActiveGameEntity.DAILY_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(
        showObjectives = true,
        showTarget = true
    )

    override fun createSession(ctx: ModeSessionContext): GameState {
        val target = ctx.targetScore.coerceAtLeast(DEFAULT_TARGET)
        val base = ctx.gameEngine.createInitialState(
            level = 1,
            bestScore = ctx.bestScore,
            seed = ctx.seed
        )
        val objective = LevelObjective.ScoreInMoves(
            id = "daily_score",
            score = target.toLong(),
            moves = MAX_MOVES
        )
        return base.copy(
            modeId = id,
            objectives = listOf(objective),
            targetValue = target,
            sessionDateKey = ctx.dateKey
        )
    }

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        var state = result.state.copy(modeId = id)
        val events = result.events.toMutableList()

        return when {
            ObjectiveEvaluator.allComplete(state) -> {
                if (events.none { it is GameEvent.LevelCompleted }) {
                    events.add(
                        GameEvent.LevelCompleted(
                            level = state.level,
                            score = state.score,
                            maxTileValue = state.board.activeTiles().maxOfOrNull { it.value } ?: 0
                        )
                    )
                }
                ModeTurnOutcome(state, events, ModeEndReason.WON)
            }
            ObjectiveEvaluator.anyFailed(state) || engine.isGameOver(state) -> {
                state = state.copy(isGameOver = true)
                if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
                ModeTurnOutcome(state, events, ModeEndReason.LOST)
            }
            else -> ModeTurnOutcome(state, events)
        }
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects =
        ModeEndEffects(finishDailyAttempt = true, recordBestScore = true)

    companion object {
        const val DEFAULT_TARGET = 3000
        const val MAX_MOVES = 80
    }
}
