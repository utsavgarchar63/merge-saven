package com.mergeseven.game.competitive

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.replay.ReplayRunner
import kotlinx.coroutines.tasks.await
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

/**
 * Calls Cloud Function `validateDailyScore`. Falls back to local [ReplayRunner] when Functions
 * are unavailable so debug builds still exercise the accept/reject path.
 */
@Singleton
class FirebaseDailyScoreValidator @Inject constructor(
    private val replayRunner: ReplayRunner
) : DailyScoreValidator {

    override suspend fun validate(submission: ScoreSubmission, playerId: String): ValidationOutcome {
        if (submission.modeId != ModeIds.DAILY) {
            return ValidationOutcome(accepted = true)
        }
        return try {
            val functions: FirebaseFunctions = Firebase.functions
            val payload = hashMapOf(
                "playerId" to playerId,
                "score" to submission.score,
                "seed" to submission.seed,
                "dateKey" to submission.dateKey,
                "moves" to submission.moves,
                "maxTile" to submission.maxTile,
                "clientVersion" to submission.clientVersion,
                "replayJson" to submission.replay.encode()
            )
            val result = functions
                .getHttpsCallable("validateDailyScore")
                .call(payload)
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: emptyMap()
            val accepted = data["accepted"] as? Boolean ?: false
            val reason = data["reason"] as? String ?: ""
            ValidationOutcome(accepted = accepted, reason = reason)
        } catch (e: Exception) {
            Log.w(TAG, "Cloud validate unavailable, using local replay: ${e.message}")
            localValidate(submission)
        }
    }

    override suspend fun fetchDailyLeaderGhost(dateKey: String): String? {
        return try {
            val functions: FirebaseFunctions = Firebase.functions
            val result = functions
                .getHttpsCallable("fetchDailyLeaderGhost")
                .call(hashMapOf("dateKey" to dateKey))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: return null
            data["replayJson"] as? String
        } catch (e: Exception) {
            Log.d(TAG, "No ghost available: ${e.message}")
            null
        }
    }

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
