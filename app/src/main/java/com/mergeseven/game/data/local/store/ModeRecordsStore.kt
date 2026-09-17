package com.mergeseven.game.data.local.store

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ModeRecord(
    val modeId: String,
    val bestScore: Long = 0,
    val runs: Int = 0,
    val lastPlayedAt: Long = 0L
)

interface ModeRecordsStore {
    val records: StateFlow<Map<String, ModeRecord>>
    suspend fun load()
    suspend fun recordRun(modeId: String, score: Long)
    fun bestScore(modeId: String): Long
}

@Singleton
class RoomModeRecordsStore @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider
) : ModeRecordsStore {

    private val _records = MutableStateFlow<Map<String, ModeRecord>>(emptyMap())
    override val records: StateFlow<Map<String, ModeRecord>> = _records.asStateFlow()

    override suspend fun load() = withContext(dispatchers.io) {
        val unlocked = unlockDao.getAll().filter { it.category == UnlockEntity.CATEGORY_MODE_RECORD }
        val mapped = unlocked.mapNotNull { entity ->
            if (!entity.unlockId.startsWith(PB_PREFIX)) return@mapNotNull null
            val modeId = entity.unlockId.removePrefix(PB_PREFIX)
            val runsEntity = unlocked.find { it.unlockId == runsKey(modeId) }
            modeId to ModeRecord(
                modeId = modeId,
                bestScore = entity.quantity.toLong(),
                runs = runsEntity?.quantity ?: 0,
                lastPlayedAt = entity.unlockedAt
            )
        }.toMap()
        val withRuns = unlocked.filter { it.unlockId.startsWith(RUNS_PREFIX) }
            .fold(mapped.toMutableMap()) { acc, entity ->
                val modeId = entity.unlockId.removePrefix(RUNS_PREFIX)
                if (acc[modeId] == null) {
                    acc[modeId] = ModeRecord(
                        modeId = modeId,
                        runs = entity.quantity,
                        lastPlayedAt = entity.unlockedAt
                    )
                }
                acc
            }
        _records.value = withRuns
    }

    override suspend fun recordRun(modeId: String, score: Long) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val current = _records.value[modeId] ?: ModeRecord(modeId)
        val best = maxOf(current.bestScore, score)
        val runs = current.runs + 1
        unlockDao.upsert(
            UnlockEntity(
                unlockId = pbKey(modeId),
                category = UnlockEntity.CATEGORY_MODE_RECORD,
                quantity = best.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                unlockedAt = now
            )
        )
        unlockDao.upsert(
            UnlockEntity(
                unlockId = runsKey(modeId),
                category = UnlockEntity.CATEGORY_MODE_RECORD,
                quantity = runs,
                unlockedAt = now
            )
        )
        _records.value = _records.value + (modeId to ModeRecord(modeId, best, runs, now))
    }

    override fun bestScore(modeId: String): Long =
        _records.value[modeId]?.bestScore ?: 0L

    private fun pbKey(modeId: String) = PB_PREFIX + modeId
    private fun runsKey(modeId: String) = RUNS_PREFIX + modeId

    private companion object {
        const val PB_PREFIX = "mode_pb_"
        const val RUNS_PREFIX = "mode_runs_"
    }
}
