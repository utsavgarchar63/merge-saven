package com.mergeseven.game.data.model

/**
 * Data model for daily quests in Merge Seven.
 */
data class DailyQuest(
    val id: String,
    val title: String,
    val description: String = "",
    val currentProgress: Int = 0,
    val targetProgress: Int,
    val coinsReward: Int,
    val starsReward: Int = 0,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean
        get() = currentProgress >= targetProgress
}
