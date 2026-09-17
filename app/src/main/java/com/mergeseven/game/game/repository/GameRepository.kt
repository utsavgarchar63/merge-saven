package com.mergeseven.game.game.repository

import android.util.Log
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.ActiveGameDao
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.data.local.snapshot.GameStateSnapshot
import com.mergeseven.game.data.local.snapshot.toGameState
import com.mergeseven.game.data.local.snapshot.toSnapshot
import com.mergeseven.game.game.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for the in-progress game, so a player who is interrupted can pick the same board
 * back up.
 */
interface GameRepository {
    /** Emits the saved game for [slotId], or null when there is nothing to resume. */
    fun getActiveGame(slotId: String = ActiveGameEntity.CAMPAIGN_SLOT): Flow<GameState?>

    /** One-shot read, for deciding at launch whether to offer "Continue". */
    suspend fun loadActiveGame(slotId: String = ActiveGameEntity.CAMPAIGN_SLOT): GameState?

    suspend fun saveActiveGame(state: GameState, slotId: String = ActiveGameEntity.CAMPAIGN_SLOT)

    suspend fun clearActiveGame(slotId: String = ActiveGameEntity.CAMPAIGN_SLOT)
}

@Singleton
class RoomGameRepository @Inject constructor(
    private val activeGameDao: ActiveGameDao,
    private val dispatchers: DispatcherProvider
) : GameRepository {

    override fun getActiveGame(slotId: String): Flow<GameState?> =
        activeGameDao.observe(slotId).map { entity -> entity?.let(::decode) }

    override suspend fun loadActiveGame(slotId: String): GameState? =
        withContext(dispatchers.io) {
            val entity = activeGameDao.get(slotId) ?: return@withContext null
            val state = decode(entity)
            // A save we cannot read is worse than no save: drop it so the player gets a clean start
            // instead of the same failure on every launch.
            if (state == null) activeGameDao.delete(slotId)
            state
        }

    override suspend fun saveActiveGame(state: GameState, slotId: String) {
        withContext(dispatchers.io) {
            val snapshot = state.toSnapshot()
            activeGameDao.upsert(
                ActiveGameEntity(
                    slotId = slotId,
                    levelId = state.level,
                    schemaVersion = GameStateSnapshot.CURRENT_VERSION,
                    snapshotJson = GameStateSnapshot.json.encodeToString(
                        GameStateSnapshot.serializer(),
                        snapshot
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun clearActiveGame(slotId: String) {
        withContext(dispatchers.io) { activeGameDao.delete(slotId) }
    }

    private fun decode(entity: ActiveGameEntity): GameState? {
        if (entity.schemaVersion !in GameStateSnapshot.MIN_SUPPORTED_VERSION..GameStateSnapshot.CURRENT_VERSION) {
            Log.w(TAG, "Discarding save: schema ${entity.schemaVersion}, expected ${GameStateSnapshot.MIN_SUPPORTED_VERSION}..${GameStateSnapshot.CURRENT_VERSION}")
            return null
        }
        return runCatching {
            GameStateSnapshot.json
                .decodeFromString(GameStateSnapshot.serializer(), entity.snapshotJson)
                .toGameState()
        }.onFailure { error ->
            Log.w(TAG, "Discarding unreadable save for slot ${entity.slotId}", error)
        }.getOrNull()
    }

    private companion object {
        const val TAG = "GameRepository"
    }
}
