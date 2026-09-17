package com.mergeseven.game.competitive

import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.replay.Replay
import com.mergeseven.game.game.replay.ReplayAction
import com.mergeseven.game.game.engine.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GhostFrame(
    val actionIndex: Int,
    val board: BoardState,
    val score: Long
)

/**
 * Steps a leader's Daily replay for read-only overlay (AF8-07).
 */
@Singleton
class GhostReplayController @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val dailyValidator: DailyScoreValidator,
    private val engine: GameEngine,
    private val analytics: AnalyticsTracker
) {
    suspend fun loadDailyLeaderFrames(dateKey: String): List<GhostFrame> =
        withContext(Dispatchers.Default) {
            if (!featureFlags.isEnabled(Feature.AF8)) return@withContext emptyList()
            val json = dailyValidator.fetchDailyLeaderGhost(dateKey) ?: return@withContext emptyList()
            val replay = runCatching { Replay.decode(json) }.getOrNull()
                ?: return@withContext emptyList()
            analytics.logEvent(
                AnalyticsEvents.GHOST_REPLAY_STARTED,
                mapOf("date" to dateKey, "actions" to replay.actions.size.toString())
            )
            buildFrames(replay)
        }

    fun buildFrames(replay: Replay): List<GhostFrame> {
        val frames = mutableListOf<GhostFrame>()
        var state = engine.createInitialState(level = replay.level, seed = replay.seed)
        frames += GhostFrame(actionIndex = -1, board = state.board, score = state.score)
        replay.actions.forEachIndexed { index, action ->
            val next = apply(state, action) ?: return@forEachIndexed
            state = next
            frames += GhostFrame(actionIndex = index, board = state.board, score = state.score)
        }
        return frames
    }

    private fun apply(state: GameState, action: ReplayAction): GameState? = when (action) {
        is ReplayAction.Place -> {
            val piece = state.trayPieces.getOrNull(action.slotIndex) ?: return null
            if (!engine.canPlace(state, piece, action.origin)) null
            else engine.placePiece(state, piece, action.origin, action.slotIndex).state
        }
        is ReplayAction.Rotate -> engine.rotateTrayPiece(state, action.slotIndex)
        is ReplayAction.RemoveTile -> engine.removeTile(state, action.cell)
        ReplayAction.Shuffle -> engine.shuffleTray(state)
        ReplayAction.Undo -> engine.undo(state)
    }
}
