package com.mergeseven.game.competitive

import androidx.annotation.StringRes
import com.mergeseven.game.R
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.replay.Replay

enum class LeaderboardId(
    val modeId: String,
    @StringRes val playGamesIdRes: Int
) {
    ENDLESS(ModeIds.ENDLESS, R.string.leaderboard_endless),
    DAILY(ModeIds.DAILY, R.string.leaderboard_daily),
    WEEKLY(ModeIds.WEEKLY, R.string.leaderboard_weekly);

    companion object {
        fun forMode(modeId: String): LeaderboardId? = entries.find { it.modeId == modeId }
    }
}

enum class LeaderboardScope {
    GLOBAL,
    FRIENDS,
    ME
}

data class LeaderboardEntry(
    val rank: Long,
    val playerId: String,
    val displayName: String,
    val score: Long,
    val isLocalPlayer: Boolean = false
)

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
