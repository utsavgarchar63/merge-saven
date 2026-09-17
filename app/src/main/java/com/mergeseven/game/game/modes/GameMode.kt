package com.mergeseven.game.game.modes

import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState

enum class ModeEndReason {
    WON,
    LOST
}

data class ModeTurnOutcome(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val endReason: ModeEndReason? = null
)

data class ModeEndEffects(
    val starsEarned: Int = 0,
    val recordBestScore: Boolean = false,
    val finishDailyAttempt: Boolean = false,
    val finishWeeklyAttempt: Boolean = false
)

data class ModeSessionContext(
    val levelId: Int,
    val seed: Long,
    val bestScore: Long,
    val dateKey: String = "",
    val targetScore: Int = 0,
    val gameEngine: GameEngine
)

/**
 * Per-mode rules, HUD, and session lifecycle (AF2-01).
 * ViewModel calls this instead of branching on mode id.
 */
interface GameMode {
    val id: String
    val saveSlotId: String
    val hud: ModeHudConfig

    fun createSession(ctx: ModeSessionContext): GameState

    fun afterMove(prev: GameState, result: GameResult, engine: GameEngine): ModeTurnOutcome

    fun onSessionEnded(state: GameState, outcome: ModeEndReason): ModeEndEffects

    /** Optional timer tick (Time Attack). Default: no-op continue. */
    fun onTimerTick(state: GameState, elapsedMs: Long, engine: GameEngine): ModeTurnOutcome =
        ModeTurnOutcome(state = state)
}
