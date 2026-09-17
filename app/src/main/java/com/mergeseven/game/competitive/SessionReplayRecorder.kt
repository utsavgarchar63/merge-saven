package com.mergeseven.game.competitive

import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.replay.Replay
import com.mergeseven.game.game.replay.ReplayAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records player inputs for a single run so the final board can be reconstructed (AF8).
 */
@Singleton
class SessionReplayRecorder @Inject constructor() {

    private val actions = mutableListOf<ReplayAction>()
    private var seed: Long = 0L
    private var level: Int = 1
    private var incomplete: Boolean = false

    fun start(seed: Long, level: Int) {
        this.seed = seed
        this.level = level
        actions.clear()
        incomplete = false
    }

    /** Call when resuming a save — we cannot reconstruct prior inputs. */
    fun markIncomplete() {
        incomplete = true
        actions.clear()
    }

    fun recordPlace(slotIndex: Int, origin: HexCoord) {
        if (incomplete) return
        actions += ReplayAction.Place(slotIndex, origin)
    }

    fun recordRotate(slotIndex: Int) {
        if (incomplete) return
        actions += ReplayAction.Rotate(slotIndex)
    }

    fun recordRemove(cell: HexCoord) {
        if (incomplete) return
        actions += ReplayAction.RemoveTile(cell)
    }

    fun recordShuffle() {
        if (incomplete) return
        actions += ReplayAction.Shuffle
    }

    fun recordUndo() {
        if (incomplete) return
        actions += ReplayAction.Undo
    }

    fun canSubmit(): Boolean = !incomplete

    fun buildReplay(): Replay? {
        if (incomplete) return null
        return Replay(seed = seed, level = level, actions = actions.toList())
    }

    fun actionCount(): Int = actions.size
}
