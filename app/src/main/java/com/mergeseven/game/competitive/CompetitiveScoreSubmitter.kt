package com.mergeseven.game.competitive

import com.mergeseven.game.BuildConfig
import com.mergeseven.game.cloud.PlayGamesAuth
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import com.mergeseven.game.game.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

enum class SubmitStatus {
    SKIPPED,
    ACCEPTED,
    REJECTED,
    NEEDS_SIGN_IN,
    RATE_LIMITED
}

data class SubmitResult(
    val status: SubmitStatus,
    val reason: String = ""
)

/**
 * End-of-run competitive submit pipeline (AF8-01..03, AF8-08).
 */
@Singleton
class CompetitiveScoreSubmitter @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val recorder: SessionReplayRecorder,
    private val sanity: ScoreSanityChecker,
    private val rateLimiter: SubmitRateLimiter,
    private val leaderboards: LeaderboardRepository,
    private val dailyValidator: DailyScoreValidator,
    private val auth: PlayGamesAuth,
    private val analytics: AnalyticsTracker
) {

    suspend fun maybeSubmit(state: GameState, modeId: String): SubmitResult {
        if (!featureFlags.isEnabled(Feature.AF8)) {
            return SubmitResult(SubmitStatus.SKIPPED, "flag_off")
        }
        val board = LeaderboardId.forMode(modeId) ?: return SubmitResult(SubmitStatus.SKIPPED, "mode")
        if (!recorder.canSubmit()) {
            return SubmitResult(SubmitStatus.SKIPPED, "incomplete_replay")
        }
        val replay = recorder.buildReplay()
            ?: return SubmitResult(SubmitStatus.SKIPPED, "no_replay")

        val dayKey = state.sessionDateKey.ifEmpty {
            when (modeId) {
                ModeIds.DAILY -> ModeSeeds.formatToday()
                ModeIds.WEEKLY -> ModeSeeds.weekKey(ModeSeeds.formatToday())
                else -> ModeSeeds.formatToday()
            }
        }

        analytics.logEvent(
            AnalyticsEvents.SCORE_SUBMIT_ATTEMPTED,
            mapOf("mode" to modeId, "score" to state.score.toString())
        )

        if (!auth.isSignedIn) {
            analytics.logEvent(
                AnalyticsEvents.SCORE_SUBMIT_REJECTED,
                mapOf("reason" to "needs_sign_in")
            )
            return SubmitResult(SubmitStatus.NEEDS_SIGN_IN)
        }

        val now = System.currentTimeMillis()
        val last = rateLimiter.lastSubmitAtMs(modeId)
        if (now - last < SubmitRateLimiter.MIN_COOLDOWN_MS) {
            analytics.logEvent(
                AnalyticsEvents.SCORE_SUBMIT_REJECTED,
                mapOf("reason" to "cooldown")
            )
            return SubmitResult(SubmitStatus.RATE_LIMITED, "cooldown")
        }
        if (!rateLimiter.tryAcquire(modeId, dayKey)) {
            analytics.logEvent(
                AnalyticsEvents.SCORE_SUBMIT_REJECTED,
                mapOf("reason" to "daily_cap")
            )
            return SubmitResult(SubmitStatus.RATE_LIMITED, "daily_cap")
        }

        val submission = ScoreSubmission(
            modeId = modeId,
            score = state.score,
            seed = state.rng.seed,
            dateKey = dayKey,
            replay = replay,
            maxTile = state.board.activeTiles().maxOfOrNull { it.value } ?: 0,
            moves = state.moves,
            clientVersion = BuildConfig.VERSION_NAME
        )

        when (val check = sanity.check(submission)) {
            is SanityResult.Rejected -> {
                analytics.logEvent(
                    AnalyticsEvents.SCORE_SUBMIT_REJECTED,
                    mapOf("reason" to check.reason)
                )
                return SubmitResult(SubmitStatus.REJECTED, check.reason)
            }
            SanityResult.Ok -> Unit
        }

        val playerId = auth.account.value?.playerId.orEmpty()
        if (modeId == ModeIds.DAILY) {
            val outcome = dailyValidator.validate(submission, playerId)
            if (!outcome.accepted) {
                analytics.logEvent(
                    AnalyticsEvents.SCORE_SUBMIT_REJECTED,
                    mapOf("reason" to outcome.reason)
                )
                return SubmitResult(SubmitStatus.REJECTED, outcome.reason)
            }
        }

        val ok = leaderboards.submitScore(board, submission.score)
        rateLimiter.markSubmitted(modeId, now)
        analytics.logEvent(
            AnalyticsEvents.SCORE_SUBMIT_ACCEPTED,
            mapOf(
                "mode" to modeId,
                "score" to submission.score.toString(),
                "pgs" to ok.toString()
            )
        )
        return SubmitResult(SubmitStatus.ACCEPTED)
    }
}
