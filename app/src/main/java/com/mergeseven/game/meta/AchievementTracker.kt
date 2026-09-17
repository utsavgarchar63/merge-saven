package com.mergeseven.game.meta

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AchievementProgress(
    val def: AchievementDef,
    val progress: Int,
    val isComplete: Boolean
)

@Singleton
class AchievementTracker @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider
) {
    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progress: StateFlow<Map<String, Int>> = _progress.asStateFlow()

    private val _newlyCompleted = MutableStateFlow<AchievementDef?>(null)
    val newlyCompleted: StateFlow<AchievementDef?> = _newlyCompleted.asStateFlow()

    suspend fun load() = withContext(dispatchers.io) {
        val map = unlockDao.getAll()
            .filter { it.category == UnlockEntity.CATEGORY_ACHIEVEMENT }
            .associate { it.unlockId.removePrefix("ach_") to it.quantity }
        _progress.value = AchievementCatalog.all.associate { it.id to (map[it.id] ?: 0) }
    }

    fun snapshot(): List<AchievementProgress> =
        AchievementCatalog.all.map { def ->
            val p = _progress.value[def.id] ?: 0
            AchievementProgress(def, p.coerceAtMost(def.target), p >= def.target)
        }

    /**
     * Sets absolute progress for [metric] (max with existing). Returns newly completed defs.
     */
    suspend fun reportMetric(metric: AchievementMetric, value: Int): List<AchievementDef> =
        withContext(dispatchers.io) {
            val completed = mutableListOf<AchievementDef>()
            for (def in AchievementCatalog.all.filter { it.metric == metric }) {
                val current = _progress.value[def.id] ?: 0
                if (current >= def.target) continue
                val next = when (metric) {
                    AchievementMetric.BIGGEST_TILE,
                    AchievementMetric.STREAK,
                    AchievementMetric.SCORE,
                    AchievementMetric.LEVELS_CLEARED -> maxOf(current, value)
                    AchievementMetric.MERGES -> current + value.coerceAtLeast(0)
                }
                if (next == current) continue
                unlockDao.upsert(
                    UnlockEntity(
                        unlockId = AchievementCatalog.unlockId(def.id),
                        category = UnlockEntity.CATEGORY_ACHIEVEMENT,
                        quantity = next,
                        unlockedAt = System.currentTimeMillis()
                    )
                )
                _progress.value = _progress.value + (def.id to next)
                if (current < def.target && next >= def.target) {
                    completed += def
                    _newlyCompleted.value = def
                }
            }
            completed
        }

    fun consumeToast() {
        _newlyCompleted.value = null
    }
}
