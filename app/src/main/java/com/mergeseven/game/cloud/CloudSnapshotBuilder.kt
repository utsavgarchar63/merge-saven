package com.mergeseven.game.cloud

import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.store.LevelProgress
import com.mergeseven.game.data.local.store.LevelProgressStore
import com.mergeseven.game.data.local.store.UserProfileStore
import com.mergeseven.game.data.model.DailyChallengeState
import com.mergeseven.game.data.model.DailyQuest
import com.mergeseven.game.data.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSnapshotBuilder @Inject constructor(
    private val userProfileStore: UserProfileStore,
    private val levelProgressStore: LevelProgressStore,
    private val unlockDao: UnlockDao,
    private val deviceIdStore: DeviceIdStore
) {
    suspend fun build(nowEpochMs: Long = System.currentTimeMillis()): CloudSnapshot {
        val profile = userProfileStore.load() ?: UserProfile()
        val levels = levelProgressStore.load().values.sortedBy { it.levelNumber }
        val unlocks = unlockDao.getAll()
        return CloudSnapshot(
            schemaVersion = CloudSnapshot.SCHEMA_VERSION,
            deviceId = deviceIdStore.getOrCreate(),
            updatedAtEpochMs = nowEpochMs,
            profile = profile.toPayload(),
            levels = levels.map { it.toPayload() },
            unlocks = unlocks.map {
                UnlockPayload(
                    unlockId = it.unlockId,
                    category = it.category,
                    quantity = it.quantity,
                    unlockedAt = it.unlockedAt
                )
            }
        )
    }
}

internal fun UserProfile.toPayload() = UserProfilePayload(
    coins = coins,
    totalStars = totalStars,
    currentStreak = currentStreak,
    claimedDays = claimedDays.sorted(),
    lastLoginDate = lastLoginDate,
    dailyQuests = dailyQuests.map { it.toPayload() },
    dailyChallenge = dailyChallenge.toPayload(),
    xp = xp,
    playerLevel = playerLevel,
    equippedTileThemeId = equippedTileThemeId,
    equippedBoardThemeId = equippedBoardThemeId,
    totalMerges = totalMerges,
    biggestTile = biggestTile,
    longestChain = longestChain,
    playtimeMs = playtimeMs
)

internal fun DailyQuest.toPayload() = DailyQuestPayload(
    id = id,
    title = title,
    description = description,
    currentProgress = currentProgress,
    targetProgress = targetProgress,
    coinsReward = coinsReward,
    starsReward = starsReward,
    isClaimed = isClaimed
)

internal fun DailyChallengeState.toPayload() = DailyChallengePayload(
    dateSeed = dateSeed,
    title = title,
    targetScore = targetScore,
    isCompleted = isCompleted,
    bestScore = bestScore,
    coinsReward = coinsReward,
    starsReward = starsReward,
    attempts = attempts
)

internal fun LevelProgress.toPayload() = LevelProgressPayload(
    levelNumber = levelNumber,
    stars = stars,
    bestScore = bestScore,
    isCompleted = isCompleted,
    failCount = failCount
)

internal fun UserProfilePayload.toDomain() = UserProfile(
    coins = coins,
    totalStars = totalStars,
    currentStreak = currentStreak,
    claimedDays = claimedDays.toSet(),
    lastLoginDate = lastLoginDate,
    dailyQuests = dailyQuests.map { it.toDomain() },
    dailyChallenge = dailyChallenge.toDomain(),
    xp = xp,
    playerLevel = playerLevel,
    equippedTileThemeId = equippedTileThemeId,
    equippedBoardThemeId = equippedBoardThemeId,
    totalMerges = totalMerges,
    biggestTile = biggestTile,
    longestChain = longestChain,
    playtimeMs = playtimeMs
)

internal fun DailyQuestPayload.toDomain() = DailyQuest(
    id = id,
    title = title,
    description = description,
    currentProgress = currentProgress,
    targetProgress = targetProgress,
    coinsReward = coinsReward,
    starsReward = starsReward,
    isClaimed = isClaimed
)

internal fun DailyChallengePayload.toDomain() = DailyChallengeState(
    dateSeed = dateSeed,
    title = title,
    targetScore = targetScore,
    isCompleted = isCompleted,
    bestScore = bestScore,
    coinsReward = coinsReward,
    starsReward = starsReward,
    attempts = attempts
)

internal fun LevelProgressPayload.toDomain() = LevelProgress(
    levelNumber = levelNumber,
    stars = stars,
    bestScore = bestScore,
    isCompleted = isCompleted,
    failCount = failCount
)
