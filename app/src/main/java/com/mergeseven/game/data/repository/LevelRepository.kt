package com.mergeseven.game.data.repository

import com.mergeseven.game.data.local.store.LevelProgress
import com.mergeseven.game.data.local.store.LevelProgressStore
import com.mergeseven.game.di.PersistenceScope
import com.mergeseven.game.game.rules.LevelPool
import com.mergeseven.game.game.rules.LevelRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** A level as shown in the level map: its rule plus the player's record on it. */
data class LevelItem(
    val rule: LevelRule,
    val isUnlocked: Boolean,
    val stars: Int,
    val bestScore: Long,
    val isCompleted: Boolean
)

/**
 * Level progression: which levels are unlocked, and the player's best result on each.
 *
 * Like [UserDataRepository], reads come from memory and writes go through to storage. Progress is
 * monotonic (stars and scores only ever increase), so a mutation racing with the initial load is
 * resolved by taking the better of the two rather than by replaying anything.
 */
@Singleton
class LevelRepository @Inject constructor(
    private val store: LevelProgressStore,
    @PersistenceScope private val scope: CoroutineScope
) {

    private val progress = MutableStateFlow<Map<Int, LevelProgress>>(emptyMap())

    val highestUnlockedLevel: Flow<Int> = progress.map { it.highestUnlocked() }

    init {
        scope.launch {
            val stored = store.load()
            progress.update { local -> mergeProgress(stored, local) }
        }
    }

    fun getLevels(totalLevels: Int = DEFAULT_LEVEL_COUNT): List<LevelItem> {
        val current = progress.value
        val maxUnlocked = current.highestUnlocked()

        return (1..totalLevels).map { levelNumber ->
            val record = current[levelNumber]
            LevelItem(
                rule = LevelPool.getLevel(levelNumber),
                isUnlocked = levelNumber <= maxUnlocked,
                stars = record?.stars ?: 0,
                bestScore = record?.bestScore ?: 0L,
                isCompleted = record?.isCompleted ?: false
            )
        }
    }

    fun completeLevel(levelNum: Int, score: Long, starsEarned: Int) {
        val existing = progress.value[levelNum]
        val updated = LevelProgress(
            levelNumber = levelNum,
            stars = maxOf(starsEarned, existing?.stars ?: 0),
            bestScore = maxOf(score, existing?.bestScore ?: 0L),
            isCompleted = true,
            failCount = existing?.failCount ?: 0
        )

        if (updated == existing) return

        progress.update { it + (levelNum to updated) }
        scope.launch { store.save(updated) }
    }

    /** AF4-07: increment campaign fail count for adaptive spawn. */
    fun recordLevelFail(levelNum: Int) {
        val existing = progress.value[levelNum]
        val updated = LevelProgress(
            levelNumber = levelNum,
            stars = existing?.stars ?: 0,
            bestScore = existing?.bestScore ?: 0L,
            isCompleted = existing?.isCompleted ?: false,
            failCount = (existing?.failCount ?: 0) + 1
        )
        progress.update { it + (levelNum to updated) }
        scope.launch { store.save(updated) }
    }

    fun failCount(levelNum: Int): Int = progress.value[levelNum]?.failCount ?: 0

    /**
     * Debug / QA helper: unlocks [level] by marking every prior level completed (if not already).
     * Existing stars and scores are preserved; missing records get a 1-star stub.
     */
    fun unlockThrough(level: Int) {
        require(level >= 1) { "level must be >= 1, was $level" }
        if (level == 1) return

        for (levelNum in 1 until level) {
            val existing = progress.value[levelNum]
            if (existing?.isCompleted == true) continue
            completeLevel(
                levelNum = levelNum,
                score = existing?.bestScore ?: 0L,
                starsEarned = maxOf(1, existing?.stars ?: 0)
            )
        }
    }

    fun totalStars(totalLevels: Int = DEFAULT_LEVEL_COUNT): Int =
        progress.value.values
            .filter { it.levelNumber in 1..totalLevels }
            .sumOf { it.stars }

    private fun Map<Int, LevelProgress>.highestUnlocked(): Int =
        (keys.filter { this[it]?.isCompleted == true }.maxOrNull() ?: 0) + 1

    /** Keeps the better record for each level; nothing the player earned is ever discarded. */
    private fun mergeProgress(
        stored: Map<Int, LevelProgress>,
        local: Map<Int, LevelProgress>
    ): Map<Int, LevelProgress> =
        (stored.keys + local.keys).associateWith { levelNumber ->
            val a = stored[levelNumber]
            val b = local[levelNumber]
            when {
                a == null -> requireNotNull(b)
                b == null -> a
                else -> LevelProgress(
                    levelNumber = levelNumber,
                    stars = maxOf(a.stars, b.stars),
                    bestScore = maxOf(a.bestScore, b.bestScore),
                    isCompleted = a.isCompleted || b.isCompleted,
                    failCount = maxOf(a.failCount, b.failCount)
                )
            }
        }

    private companion object {
        const val DEFAULT_LEVEL_COUNT = 30
    }
}
