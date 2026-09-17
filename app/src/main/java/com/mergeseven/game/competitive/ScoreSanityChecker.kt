package com.mergeseven.game.competitive

import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side score gates before any network submit (AF8-02).
 */
@Singleton
class ScoreSanityChecker @Inject constructor() {

    fun check(submission: ScoreSubmission): SanityResult {
        if (submission.score < 0L) {
            return SanityResult.Rejected("negative_score")
        }
        if (submission.score > MAX_SCORE) {
            return SanityResult.Rejected("score_overflow")
        }
        if (submission.moves < 0 || submission.moves > MAX_MOVES) {
            return SanityResult.Rejected("invalid_moves")
        }
        if (submission.maxTile < 0 || submission.maxTile > MAX_TILE) {
            return SanityResult.Rejected("invalid_max_tile")
        }
        // Crude ceiling: cannot earn more than this per move on average.
        val theoretical = submission.moves.toLong() * MAX_SCORE_PER_MOVE + BASELINE_SCORE
        if (submission.score > theoretical) {
            return SanityResult.Rejected("score_vs_moves")
        }
        if (submission.replay.seed != submission.seed) {
            return SanityResult.Rejected("seed_mismatch")
        }
        when (submission.modeId) {
            ModeIds.DAILY -> {
                if (submission.dateKey.isBlank()) {
                    return SanityResult.Rejected("missing_date")
                }
                val expected = ModeSeeds.dailySeed(submission.dateKey)
                if (submission.seed != expected) {
                    return SanityResult.Rejected("daily_seed_mismatch")
                }
            }
            ModeIds.WEEKLY -> {
                if (submission.dateKey.isBlank()) {
                    return SanityResult.Rejected("missing_week")
                }
                val expected = ModeSeeds.weeklySeed(submission.dateKey)
                if (submission.seed != expected) {
                    return SanityResult.Rejected("weekly_seed_mismatch")
                }
            }
            ModeIds.ENDLESS -> Unit
            else -> return SanityResult.Rejected("unsupported_mode")
        }
        return SanityResult.Ok
    }

    companion object {
        const val MAX_SCORE = 50_000_000L
        const val MAX_MOVES = 10_000
        const val MAX_TILE = 1_048_576
        const val MAX_SCORE_PER_MOVE = 50_000L
        const val BASELINE_SCORE = 100_000L
    }
}
