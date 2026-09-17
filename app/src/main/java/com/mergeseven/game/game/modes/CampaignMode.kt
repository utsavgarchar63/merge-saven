package com.mergeseven.game.game.modes

import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.objectives.ObjectiveEvaluator
import com.mergeseven.game.game.objectives.StarRating
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.CAMPAIGN
    override val saveSlotId: String = ActiveGameEntity.CAMPAIGN_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(
        showObjectives = true,
        showTarget = true
    )

    override fun createSession(ctx: ModeSessionContext): GameState =
        ctx.gameEngine.createInitialState(
            level = ctx.levelId,
            bestScore = ctx.bestScore,
            seed = ctx.seed
        ).copy(modeId = id)

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        var state = result.state.copy(modeId = id)
        val events = result.events.toMutableList()

        return when {
            ObjectiveEvaluator.anyFailed(state) -> {
                state = state.copy(isGameOver = true)
                if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
                ModeTurnOutcome(state, events, ModeEndReason.LOST)
            }
            ObjectiveEvaluator.allComplete(state) -> {
                val maxTile = state.board.activeTiles().maxOfOrNull { it.value } ?: 0
                if (events.none { it is GameEvent.LevelCompleted }) {
                    events.add(GameEvent.LevelCompleted(state.level, state.score, maxTile))
                }
                ModeTurnOutcome(state, events, ModeEndReason.WON)
            }
            engine.isGameOver(state) -> {
                state = state.copy(isGameOver = true)
                if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
                ModeTurnOutcome(state, events, ModeEndReason.LOST)
            }
            else -> ModeTurnOutcome(state, events)
        }
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects {
        if (outcome != ModeEndReason.WON) return ModeEndEffects()
        return ModeEndEffects(starsEarned = StarRating.compute(state))
    }
}
