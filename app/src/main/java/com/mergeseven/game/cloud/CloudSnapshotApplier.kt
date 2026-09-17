package com.mergeseven.game.cloud

import com.mergeseven.game.data.local.dao.LevelProgressDao
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.data.local.store.LevelProgressStore
import com.mergeseven.game.data.repository.UserDataRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Writes a [CloudSnapshot] into Room / in-memory profile.
 * Merge-safe apply never decreases coins, stars, unlock quantities, or per-level bests (ADV-005).
 */
@Singleton
class CloudSnapshotApplier @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val levelProgressStore: LevelProgressStore,
    private val levelProgressDao: LevelProgressDao,
    private val unlockDao: UnlockDao
) {

    /**
     * @param chosen Prefer this side for non-merge-safe fields (XP, themes, streak, dailies).
     * @param other Other side used only for merge-safe max/union.
     */
    suspend fun applyMergeSafe(chosen: CloudSnapshot, other: CloudSnapshot?) {
        val merged = if (other == null) chosen else mergeSafe(chosen, other)
        applyExact(merged)
    }

    /** Overwrite local with [snapshot] as-is (still used after mergeSafe produced the blob). */
    suspend fun applyExact(snapshot: CloudSnapshot) {
        userDataRepository.replaceFromCloud(snapshot.profile.toDomain())

        val existingLevels = levelProgressStore.load()
        val levelNumbers = (existingLevels.keys + snapshot.levels.map { it.levelNumber }).toSet()
        for (levelNumber in levelNumbers) {
            val fromSnap = snapshot.levels.find { it.levelNumber == levelNumber }
            val local = existingLevels[levelNumber]
            when {
                fromSnap != null && local != null -> {
                    levelProgressStore.save(
                        fromSnap.toDomain().copy(
                            stars = max(fromSnap.stars, local.stars),
                            bestScore = max(fromSnap.bestScore, local.bestScore),
                            isCompleted = fromSnap.isCompleted || local.isCompleted,
                            failCount = max(fromSnap.failCount, local.failCount)
                        )
                    )
                }
                fromSnap != null -> levelProgressStore.save(fromSnap.toDomain())
                // keep local-only rows
            }
        }

        val localUnlocks = unlockDao.getAll().associateBy { it.unlockId }
        val cloudUnlocks = snapshot.unlocks.associateBy { it.unlockId }
        val allIds = localUnlocks.keys + cloudUnlocks.keys
        for (id in allIds) {
            val cloud = cloudUnlocks[id]
            val local = localUnlocks[id]
            when {
                cloud != null && local != null -> {
                    unlockDao.upsert(
                        UnlockEntity(
                            unlockId = id,
                            category = cloud.category.ifBlank { local.category },
                            quantity = max(cloud.quantity, local.quantity),
                            unlockedAt = minOf(cloud.unlockedAt, local.unlockedAt)
                        )
                    )
                }
                cloud != null -> {
                    unlockDao.upsert(
                        UnlockEntity(
                            unlockId = cloud.unlockId,
                            category = cloud.category,
                            quantity = cloud.quantity,
                            unlockedAt = cloud.unlockedAt
                        )
                    )
                }
            }
        }
    }

    /**
     * Merge-safe union: [preferred] wins XP/themes/streak/daily; numeric progress takes max;
     * unlocks and levels are unioned.
     */
    fun mergeSafe(preferred: CloudSnapshot, other: CloudSnapshot): CloudSnapshot {
        val p = preferred.profile
        val o = other.profile
        val profile = p.copy(
            coins = max(p.coins, o.coins),
            totalStars = max(p.totalStars, o.totalStars),
            totalMerges = max(p.totalMerges, o.totalMerges),
            biggestTile = max(p.biggestTile, o.biggestTile),
            longestChain = max(p.longestChain, o.longestChain),
            playtimeMs = max(p.playtimeMs, o.playtimeMs)
        )

        val levelsByNumber = LinkedHashMap<Int, LevelProgressPayload>()
        (other.levels + preferred.levels).forEach { row ->
            val prev = levelsByNumber[row.levelNumber]
            levelsByNumber[row.levelNumber] = if (prev == null) {
                row
            } else {
                LevelProgressPayload(
                    levelNumber = row.levelNumber,
                    stars = max(prev.stars, row.stars),
                    bestScore = max(prev.bestScore, row.bestScore),
                    isCompleted = prev.isCompleted || row.isCompleted,
                    failCount = max(prev.failCount, row.failCount)
                )
            }
        }

        val unlocksById = LinkedHashMap<String, UnlockPayload>()
        (other.unlocks + preferred.unlocks).forEach { row ->
            val prev = unlocksById[row.unlockId]
            unlocksById[row.unlockId] = if (prev == null) {
                row
            } else {
                UnlockPayload(
                    unlockId = row.unlockId,
                    category = row.category.ifBlank { prev.category },
                    quantity = max(prev.quantity, row.quantity),
                    unlockedAt = minOf(prev.unlockedAt, row.unlockedAt)
                )
            }
        }

        return preferred.copy(
            profile = profile,
            levels = levelsByNumber.values.sortedBy { it.levelNumber },
            unlocks = unlocksById.values.sortedBy { it.unlockId },
            updatedAtEpochMs = max(preferred.updatedAtEpochMs, other.updatedAtEpochMs)
        )
    }

    suspend fun wipeMetaProgress() {
        userDataRepository.replaceFromCloud(com.mergeseven.game.data.model.UserProfile())
        levelProgressDao.clear()
        unlockDao.clearAll()
    }
}
