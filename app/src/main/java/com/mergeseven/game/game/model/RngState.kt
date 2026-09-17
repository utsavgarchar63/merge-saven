package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * The random state of a run, small enough to travel inside [GameState] and a save file.
 *
 * Holding this in the game state rather than inside an engine is what makes a run reproducible: the
 * same [seed] replayed through the same actions produces the same board, and a resumed save
 * continues the piece stream exactly where it stopped instead of starting a fresh one.
 *
 * @param seed the value the run started from. Never advances; kept so a run can be reproduced from
 *   a bug report or, for a daily puzzle, derived from the date.
 * @param cursor the live position in the sequence. Advances on every draw.
 * @param draws how many values have been taken. Diagnostics only — [cursor] alone determines what
 *   comes next.
 * @param nextEntityId the next id to hand out to a tile or piece. Part of the random state because
 *   ids must also be reproducible for two replays to compare equal.
 */
@Serializable
data class RngState(
    val seed: Long,
    val cursor: Long,
    val draws: Int = 0,
    val nextEntityId: Long = 1L
) {
    companion object {
        fun fromSeed(seed: Long): RngState = RngState(
            seed = seed,
            cursor = seed,
            draws = 0,
            nextEntityId = 1L
        )
    }
}
