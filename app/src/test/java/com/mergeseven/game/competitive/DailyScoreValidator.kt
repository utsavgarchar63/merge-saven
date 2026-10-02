package com.mergeseven.game.competitive

import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.replay.ReplayRunner
import javax.inject.Inject
import javax.inject.Singleton

data class ValidationOutcome(
    val accepted: Boolean,
    val reason: String = "",
    val ghostReplayJson: String? = null
)

interface DailyScoreValidator {
    suspend fun validate(submission: ScoreSubmission, playerId: String): ValidationOutcome
    suspend fun fetchDailyLeaderGhost(dateKey: String): String?
}

/** Offline replay verification; no scores or saves are uploaded. */
@Singleton
class LocalDailyScoreValidator @Inject constructor(private val replayRunner: ReplayRunner) : DailyScoreValidator {
    override suspend fun validate(submission: ScoreSubmission, playerId: String) = localValidate(submission)
    override suspend fun fetchDailyLeaderGhost(dateKey: String): String? = null
    private fun localValidate(submission: ScoreSubmission): ValidationOutcome {
        val result = replayRunner.run(submission.replay)
        if (!result.isFaithful) {
            return ValidationOutcome(false, "replay_skipped_${result.skipped}")
        }
        if (result.finalState.score != submission.score) {
            return ValidationOutcome(false, "score_mismatch")
        }
        return ValidationOutcome(true)
    }

    private companion object {
        const val TAG = "DailyScoreValidator"
    }
}

/** Test double that always runs local ReplayRunner. */
class FakeDailyScoreValidator(
    private val replayRunner: ReplayRunner,
    private val shadowBanned: Set<String> = emptySet()
) : DailyScoreValidator {
    override suspend fun validate(submission: ScoreSubmission, playerId: String): ValidationOutcome {
        if (playerId in shadowBanned) {
            return ValidationOutcome(false, "shadow_banned")
        }
        val result = replayRunner.run(submission.replay)
        if (!result.isFaithful) return ValidationOutcome(false, "replay_skipped")
        if (result.finalState.score != submission.score) {
            return ValidationOutcome(false, "score_mismatch")
        }
        return ValidationOutcome(true)
    }

    override suspend fun fetchDailyLeaderGhost(dateKey: String): String? = null
}
