package com.mergeseven.game.competitive

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.modes.ModeSeeds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class Tournament(
    val id: String,
    val seed: Long,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val prizeCoins: Int,
    val title: String = "24h Challenge"
) {
    val isActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startEpochMs until endEpochMs
        }
}

interface TournamentRepository {
    val current: StateFlow<Tournament?>
    suspend fun refresh()
    suspend fun join(tournamentId: String): Tournament?
    suspend fun claimPrizeIfEligible(tournamentId: String, playerScore: Long, topScore: Long): Boolean
}

@Singleton
class FirestoreTournamentRepository @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val liveConfig: LiveConfig,
    private val userDataRepository: UserDataRepository
) : TournamentRepository {

    private val _current = MutableStateFlow<Tournament?>(null)
    override val current: StateFlow<Tournament?> = _current.asStateFlow()

    override suspend fun refresh() {
        if (!featureFlags.isEnabled(Feature.AF8)) {
            _current.value = null
            return
        }
        val fromRemote = fetchRemote()
        _current.value = fromRemote ?: syntheticFallback()
    }

    override suspend fun join(tournamentId: String): Tournament? {
        val t = _current.value?.takeIf { it.id == tournamentId && it.isActive } ?: return null
        return t
    }

    override suspend fun claimPrizeIfEligible(
        tournamentId: String,
        playerScore: Long,
        topScore: Long
    ): Boolean {
        if (!featureFlags.isEnabled(Feature.AF3)) return false
        val t = _current.value ?: return false
        if (t.id != tournamentId) return false
        if (playerScore <= 0L || playerScore < topScore) return false
        userDataRepository.addCoins(t.prizeCoins)
        return true
    }

    private suspend fun fetchRemote(): Tournament? {
        return try {
            val db: FirebaseFirestore = Firebase.firestore
            val snap = db.collection("tournaments").document("current").get().await()
            if (!snap.exists()) return null
            Tournament(
                id = snap.getString("id") ?: return null,
                seed = snap.getLong("seed") ?: return null,
                startEpochMs = snap.getLong("startEpochMs") ?: return null,
                endEpochMs = snap.getLong("endEpochMs") ?: return null,
                prizeCoins = (snap.getLong("prizeCoins") ?: 100L).toInt(),
                title = snap.getString("title") ?: "24h Challenge"
            )
        } catch (e: Exception) {
            Log.d(TAG, "Tournament fetch failed: ${e.message}")
            null
        }
    }

    /** Deterministic local 24h window when Firestore is offline (debug / airplane). */
    private fun syntheticFallback(): Tournament {
        val now = Instant.now()
        val dayStart = now.epochSecond / 86_400L * 86_400L * 1000L
        val weekKey = ModeSeeds.weekKey(ModeSeeds.formatToday())
        return Tournament(
            id = "local_$weekKey",
            seed = ModeSeeds.weeklySeed(weekKey) xor liveConfig.revision.value.toLong(),
            startEpochMs = dayStart,
            endEpochMs = dayStart + 86_400_000L,
            prizeCoins = 50,
            title = "Daily Arena"
        )
    }

    private companion object {
        const val TAG = "TournamentRepo"
    }
}
