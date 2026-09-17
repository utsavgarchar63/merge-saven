package com.mergeseven.game.game.modes

import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.WEEKLY
    override val saveSlotId: String = ActiveGameEntity.WEEKLY_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(showTarget = false)

    override fun createSession(ctx: ModeSessionContext): GameState {
        val base = ctx.gameEngine.createInitialState(
            level = 5,
            bestScore = ctx.bestScore,
            seed = ctx.seed
        )
        // Rebuild tray with force-triple so opening pieces match the modifier.
        val random = com.mergeseven.game.game.engine.GameRandom(base.rng)
        val spawn = com.mergeseven.game.game.engine.SpawnEngine()
        val hints = com.mergeseven.game.game.engine.SpawnHints(forceTriple = true)
        val pieces = List(3) { spawn.generatePiece(base.level, random, hints) }
        return base.copy(
            modeId = id,
            objectives = emptyList(),
            targetValue = 0,
            forceTriplePieces = true,
            sessionDateKey = ctx.dateKey,
            trayPieces = pieces,
            rng = random.snapshot()
        )
    }

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        var state = result.state.copy(
            modeId = id,
            forceTriplePieces = true,
            objectives = emptyList()
        )
        val events = result.events.toMutableList()
        if (engine.isGameOver(state)) {
            state = state.copy(isGameOver = true)
            if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
            return ModeTurnOutcome(state, events, ModeEndReason.LOST)
        }
        return ModeTurnOutcome(state, events)
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects =
        ModeEndEffects(finishWeeklyAttempt = true, recordBestScore = true)
}
