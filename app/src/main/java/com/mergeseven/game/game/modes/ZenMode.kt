package com.mergeseven.game.game.modes

import com.mergeseven.game.core.Constants
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZenMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.ZEN
    override val saveSlotId: String = ActiveGameEntity.ZEN_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(
        allowGameOver = false,
        unlimitedUndo = true,
        suppressAds = true,
        paletteKey = "zen",
        musicKey = "zen"
    )

    override fun createSession(ctx: ModeSessionContext): GameState {
        val base = ctx.gameEngine.createInitialState(
            level = 1,
            bestScore = ctx.bestScore,
            seed = ctx.seed
        )
        return base.copy(
            modeId = id,
            objectives = emptyList(),
            targetValue = 0
        )
    }

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        // Never game over — keep playing even when the board is full.
        val state = result.state.copy(modeId = id, objectives = emptyList(), isGameOver = false)
        val events = result.events.filterNot { it is GameEvent.GameOver }
        return ModeTurnOutcome(state, events)
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects =
        ModeEndEffects(recordBestScore = true)

    companion object {
        /** Soft cap for Zen undo chain (vs [Constants.MAX_UNDO_HISTORY]). */
        const val UNDO_DEPTH = 50
    }
}
