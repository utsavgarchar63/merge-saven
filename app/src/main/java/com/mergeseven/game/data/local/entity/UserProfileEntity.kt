package com.mergeseven.game.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The player's wallet and daily-cycle state. Single row, always [SINGLETON_ID].
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val coins: Int,
    val totalStars: Int,
    val currentStreak: Int,
    val claimedDays: Set<Int>,
    val lastLoginDate: String,
    @Embedded(prefix = "challenge_")
    val dailyChallenge: DailyChallengeEmbedded,
    val xp: Int = 0,
    val playerLevel: Int = 1,
    val equippedTileThemeId: String = "classic",
    val equippedBoardThemeId: String = "wood",
    val totalMerges: Int = 0,
    val biggestTile: Int = 0,
    val longestChain: Int = 0,
    val playtimeMs: Long = 0L
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** Today's challenge, embedded into [UserProfileEntity] because it is 1:1 with the profile. */
data class DailyChallengeEmbedded(
    val dateSeed: String,
    val title: String,
    val targetScore: Int,
    val isCompleted: Boolean,
    val bestScore: Int,
    val coinsReward: Int,
    val starsReward: Int,
    val attempts: Int
)
