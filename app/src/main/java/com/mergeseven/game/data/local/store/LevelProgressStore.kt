package com.mergeseven.game.data.local.store

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.LevelProgressDao
import com.mergeseven.game.data.local.entity.LevelProgressEntity
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Per-level record, keyed by level number. */
data class LevelProgress(
    val levelNumber: Int,
    val stars: Int,
    val bestScore: Long,
    val isCompleted: Boolean,
    val failCount: Int = 0
)

/**
 * Storage boundary for level progression. See [UserProfileStore] for why this is split out.
 */
interface LevelProgressStore {
    suspend fun load(): Map<Int, LevelProgress>

    suspend fun save(progress: LevelProgress)
}

@Singleton
class RoomLevelProgressStore @Inject constructor(
    private val dao: LevelProgressDao,
    private val dispatchers: DispatcherProvider
) : LevelProgressStore {

    override suspend fun load(): Map<Int, LevelProgress> = withContext(dispatchers.io) {
        dao.getAll().associate { entity ->
            entity.levelNumber to LevelProgress(
                levelNumber = entity.levelNumber,
                stars = entity.stars,
                bestScore = entity.bestScore,
                isCompleted = entity.isCompleted,
                failCount = entity.failCount
            )
        }
    }

    override suspend fun save(progress: LevelProgress) = withContext(dispatchers.io) {
        dao.upsert(
            LevelProgressEntity(
                levelNumber = progress.levelNumber,
                stars = progress.stars,
                bestScore = progress.bestScore,
                isCompleted = progress.isCompleted,
                updatedAt = System.currentTimeMillis(),
                failCount = progress.failCount
            )
        )
    }
}

/** In-memory store for unit tests and Compose previews. */
class InMemoryLevelProgressStore(
    initial: Map<Int, LevelProgress> = emptyMap()
) : LevelProgressStore {

    private val stored = initial.toMutableMap()

    override suspend fun load(): Map<Int, LevelProgress> = stored.toMap()

    override suspend fun save(progress: LevelProgress) {
        stored[progress.levelNumber] = progress
    }
}
