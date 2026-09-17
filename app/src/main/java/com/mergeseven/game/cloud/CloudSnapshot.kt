package com.mergeseven.game.cloud

import kotlinx.serialization.Serializable

/**
 * Versioned meta-progress blob for Play Games Snapshots (AF7).
 * Mid-run boards ([com.mergeseven.game.data.local.entity.ActiveGameEntity]) are never included.
 */
@Serializable
data class CloudSnapshot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val deviceId: String,
    val updatedAtEpochMs: Long,
    val profile: UserProfilePayload,
    val levels: List<LevelProgressPayload> = emptyList(),
    val unlocks: List<UnlockPayload> = emptyList()
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val SNAPSHOT_NAME = "merge_seven_meta_v1"
    }
}

@Serializable
data class UserProfilePayload(
    val coins: Int,
    val totalStars: Int,
    val currentStreak: Int = 1,
    val claimedDays: List<Int> = emptyList(),
    val lastLoginDate: String = "",
    val dailyQuests: List<DailyQuestPayload> = emptyList(),
    val dailyChallenge: DailyChallengePayload = DailyChallengePayload(),
    val xp: Int = 0,
    val playerLevel: Int = 1,
    val equippedTileThemeId: String = "classic",
    val equippedBoardThemeId: String = "wood",
    val totalMerges: Int = 0,
    val biggestTile: Int = 0,
    val longestChain: Int = 0,
    val playtimeMs: Long = 0L
)

@Serializable
data class DailyQuestPayload(
    val id: String,
    val title: String,
    val description: String = "",
    val currentProgress: Int = 0,
    val targetProgress: Int,
    val coinsReward: Int,
    val starsReward: Int = 0,
    val isClaimed: Boolean = false
)

@Serializable
data class DailyChallengePayload(
    val dateSeed: String = "",
    val title: String = "DAILY CHALLENGE",
    val targetScore: Int = 3000,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val coinsReward: Int = 500,
    val starsReward: Int = 5,
    val attempts: Int = 0
)

@Serializable
data class LevelProgressPayload(
    val levelNumber: Int,
    val stars: Int,
    val bestScore: Long,
    val isCompleted: Boolean,
    val failCount: Int = 0
)

@Serializable
data class UnlockPayload(
    val unlockId: String,
    val category: String,
    val quantity: Int,
    val unlockedAt: Long
)

/** Compact summary for conflict / restore dialogs. */
data class ProgressSummary(
    val totalStars: Int,
    val completedLevelCount: Int,
    val totalMerges: Int,
    val coins: Int
)
