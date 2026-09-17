package com.mergeseven.game.game.replay

import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject

/**
 * Replays a [Replay] through the engine (AF0-15).
 *
 * Two uses:
 * - Tests assert that a seed plus a list of actions always lands on the same state, which is how a
 *   change that quietly breaks determinism gets caught.
 * - A bug report carrying a seed and actions can be turned back into the exact board the reporter
 *   was looking at.
 *
 * Illegal actions are skipped rather than throwing, because a replay recorded against a different
 * build will contain moves this build considers invalid; [ReplayResult.skipped] reports how many,
 * and a non-zero count means the replay and the engine disagree.
 */
class ReplayRunner @Inject constructor(
    private val engine: GameEngine
) {

    data class ReplayResult(
        val finalState: GameState,
        val applied: Int,
        val skipped: Int
    ) {
        val isFaithful: Boolean get() = skipped == 0
    }

    fun run(replay: Replay): ReplayResult {
        var state = engine.createInitialState(level = replay.level, seed = replay.seed)
        var applied = 0
        var skipped = 0

        for (action in replay.actions) {
            val next = apply(state, action)
            if (next == null) {
                skipped++
            } else {
                state = next
                applied++
            }
        }

        return ReplayResult(finalState = state, applied = applied, skipped = skipped)
    }

    /** Returns the resulting state, or null when the action was not legal in [state]. */
    private fun apply(state: GameState, action: ReplayAction): GameState? = when (action) {
        is ReplayAction.Place -> {
            val piece = state.trayPieces.getOrNull(action.slotIndex)
            when {
                piece == null -> null
                !engine.canPlace(state, piece, action.origin) -> null
                else -> engine.placePiece(state, piece, action.origin, action.slotIndex).state
            }
        }

        is ReplayAction.Rotate ->
            if (state.trayPieces.getOrNull(action.slotIndex) == null) {
                null
            } else {
                engine.rotateTrayPiece(state, action.slotIndex)
            }

        is ReplayAction.RemoveTile ->
            if (state.board.tileAt(action.cell) == null) null else engine.removeTile(state, action.cell)

        ReplayAction.Shuffle -> engine.shuffleTray(state)

        ReplayAction.Undo -> state.previousState?.let { engine.undo(state) }
    }
}
