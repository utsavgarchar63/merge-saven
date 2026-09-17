package com.mergeseven.game.game.modes

import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EndlessMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.ENDLESS
    override val saveSlotId: String = ActiveGameEntity.ENDLESS_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(showTarget = false)

    override fun createSession(ctx: ModeSessionContext): GameState {
        val base = ctx.gameEngine.createInitialState(
            level = 1,
            bestScore = ctx.bestScore,
            seed = ctx.seed
        )
        return base.copy(
            modeId = id,
            objectives = emptyList(),
            targetValue = 0,
            spawnTier = 0
        )
    }

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        val tier = result.state.moves / MOVES_PER_TIER
        var state = result.state.copy(modeId = id, spawnTier = tier, objectives = emptyList())
        val events = result.events.toMutableList()
        if (engine.isGameOver(state)) {
            state = state.copy(isGameOver = true)
            if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
            return ModeTurnOutcome(state, events, ModeEndReason.LOST)
        }
        return ModeTurnOutcome(state, events)
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects =
        ModeEndEffects(recordBestScore = outcome == ModeEndReason.LOST)

    companion object {
        const val MOVES_PER_TIER = 10
    }
}
