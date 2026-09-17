package com.mergeseven.game.game.modes

import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeAttackMode @Inject constructor() : GameMode {

    override val id: String = ModeIds.TIME_ATTACK
    override val saveSlotId: String = ActiveGameEntity.TIME_ATTACK_SLOT
    override val hud: ModeHudConfig = ModeHudConfig(showTimer = true)

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
            timeRemainingMs = START_TIME_MS
        )
    }

    override fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome {
        if (result.events.any { it is GameEvent.InvalidPlacement }) {
            return ModeTurnOutcome(state = result.state, events = result.events)
        }
        val mergeEvents = result.events.filterIsInstance<GameEvent.MergeCompleted>()
        var bonus = 0L
        mergeEvents.forEachIndexed { index, _ ->
            val depthBonus = index.coerceAtMost(3) * CHAIN_BONUS_MS
            bonus += (BASE_MERGE_BONUS_MS + depthBonus).coerceAtMost(MAX_BONUS_PER_MERGE_MS)
        }

        var state = result.state.copy(
            modeId = id,
            objectives = emptyList(),
            timeRemainingMs = (prev.timeRemainingMs + bonus).coerceAtLeast(0L)
        )
        // placePiece does not touch timeRemainingMs; restore from prev then add bonus
        // Actually result.state may have default 0 if not copied — engine copies full state so
        // timeRemainingMs should persist from prev via state.copy in engine. Good.

        val events = result.events.toMutableList()

        return when {
            state.timeRemainingMs <= 0L -> {
                state = state.copy(isGameOver = true, timeRemainingMs = 0L)
                if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
                ModeTurnOutcome(state, events, ModeEndReason.LOST)
            }
            engine.isGameOver(state) -> {
                state = state.copy(isGameOver = true)
                if (events.none { it is GameEvent.GameOver }) events.add(GameEvent.GameOver)
                ModeTurnOutcome(state, events, ModeEndReason.LOST)
            }
            else -> ModeTurnOutcome(state, events)
        }
    }

    override fun onTimerTick(state: GameState, elapsedMs: Long, engine: GameEngine): ModeTurnOutcome {
        if (state.isGameOver) return ModeTurnOutcome(state)
        if (state.timeFrozenMs > 0L) {
            val frozenLeft = (state.timeFrozenMs - elapsedMs).coerceAtLeast(0L)
            return ModeTurnOutcome(state = state.copy(timeFrozenMs = frozenLeft))
        }
        val remaining = (state.timeRemainingMs - elapsedMs).coerceAtLeast(0L)
        if (remaining <= 0L) {
            return ModeTurnOutcome(
                state = state.copy(timeRemainingMs = 0L, isGameOver = true),
                events = listOf(GameEvent.GameOver),
                endReason = ModeEndReason.LOST
            )
        }
        return ModeTurnOutcome(state = state.copy(timeRemainingMs = remaining))
    }

    override fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects =
        ModeEndEffects(recordBestScore = true)

    companion object {
        const val START_TIME_MS = 180_000L
        const val BASE_MERGE_BONUS_MS = 2_000L
        const val CHAIN_BONUS_MS = 1_000L
        const val MAX_BONUS_PER_MERGE_MS = 5_000L
    }
}
