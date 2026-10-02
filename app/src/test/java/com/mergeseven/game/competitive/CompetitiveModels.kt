package com.mergeseven.game.competitive

import androidx.annotation.StringRes
import com.mergeseven.game.R
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.replay.Replay

data class ScoreSubmission(
    val modeId: String,
    val score: Long,
    val seed: Long,
    val dateKey: String = "",
    val replay: Replay,
    val maxTile: Int,
    val moves: Int,
    val clientVersion: String
)

sealed class SanityResult {
    data object Ok : SanityResult()
    data class Rejected(val reason: String) : SanityResult()
}
