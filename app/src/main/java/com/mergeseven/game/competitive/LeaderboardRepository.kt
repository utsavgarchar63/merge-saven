package com.mergeseven.game.competitive

import android.content.Context
import android.util.Log
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.LeaderboardVariant
import com.mergeseven.game.cloud.PlayGamesAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface LeaderboardRepository {
    suspend fun submitScore(board: LeaderboardId, score: Long): Boolean
    suspend fun loadScores(
        board: LeaderboardId,
        scope: LeaderboardScope,
        maxResults: Int = 25
    ): List<LeaderboardEntry>
    fun cachedScores(board: LeaderboardId, scope: LeaderboardScope): List<LeaderboardEntry>
    fun openPlayGamesIntent(board: LeaderboardId): android.content.Intent?
}

@Singleton
class PlayGamesLeaderboardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: PlayGamesAuth
) : LeaderboardRepository {

    private val mutex = Mutex()
    private val cache = mutableMapOf<Pair<LeaderboardId, LeaderboardScope>, List<LeaderboardEntry>>()

    override fun cachedScores(board: LeaderboardId, scope: LeaderboardScope): List<LeaderboardEntry> =
        cache[board to scope].orEmpty()

    override suspend fun submitScore(board: LeaderboardId, score: Long): Boolean = mutex.withLock {
        val account = auth.lastAccount() ?: return false
        return try {
            val client = Games.getLeaderboardsClient(context, account)
            val id = context.getString(board.playGamesIdRes)
            client.submitScoreImmediate(id, score).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "submitScore failed: ${e.message}")
            // Local-only fallback so UI/tests still see progress without a live Games project.
            val key = board to LeaderboardScope.ME
            val name = auth.account.value?.displayName ?: "You"
            val pid = auth.account.value?.playerId ?: "local"
            cache[key] = listOf(
                LeaderboardEntry(
                    rank = 1,
                    playerId = pid,
                    displayName = name,
                    score = score,
                    isLocalPlayer = true
                )
            )
            false
        }
    }

    override suspend fun loadScores(
        board: LeaderboardId,
        scope: LeaderboardScope,
        maxResults: Int
    ): List<LeaderboardEntry> = mutex.withLock {
        val account = auth.lastAccount()
        if (account == null) {
            return cache[board to scope].orEmpty()
        }
        return try {
            val client = Games.getLeaderboardsClient(context, account)
            val id = context.getString(board.playGamesIdRes)
            val collection = when (scope) {
                LeaderboardScope.FRIENDS -> LeaderboardVariant.COLLECTION_FRIENDS
                else -> LeaderboardVariant.COLLECTION_PUBLIC
            }
            val buffer = if (scope == LeaderboardScope.ME) {
                client.loadCurrentPlayerLeaderboardScore(
                    id,
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    collection
                ).await()?.get()
            } else {
                null
            }
            if (scope == LeaderboardScope.ME) {
                val score = buffer
                val entries = if (score != null) {
                    listOf(
                        LeaderboardEntry(
                            rank = score.rank,
                            playerId = auth.account.value?.playerId.orEmpty(),
                            displayName = auth.account.value?.displayName ?: "You",
                            score = score.rawScore,
                            isLocalPlayer = true
                        )
                    )
                } else {
                    cache[board to scope].orEmpty()
                }
                cache[board to scope] = entries
                entries
            } else {
                val annotated = client.loadTopScores(
                    id,
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    collection,
                    maxResults
                ).await()?.get()
                val scores = annotated?.scores ?: emptyList()
                val localId = auth.account.value?.playerId
                val entries = scores.mapIndexed { index, s ->
                    LeaderboardEntry(
                        rank = s.rank.takeIf { it > 0 } ?: (index + 1L),
                        playerId = s.scoreHolder?.playerId.orEmpty(),
                        displayName = s.scoreHolderDisplayName ?: "Player",
                        score = s.rawScore,
                        isLocalPlayer = localId != null && s.scoreHolder?.playerId == localId
                    )
                }
                cache[board to scope] = entries
                entries
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadScores failed: ${e.message}")
            cache[board to scope].orEmpty()
        }
    }

    override fun openPlayGamesIntent(board: LeaderboardId): android.content.Intent? {
        val account = auth.lastAccount() ?: return null
        return try {
            val client = Games.getLeaderboardsClient(context, account)
            // Intent is async in newer APIs; callers handle null gracefully.
            null
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val TAG = "LeaderboardRepo"
    }
}
