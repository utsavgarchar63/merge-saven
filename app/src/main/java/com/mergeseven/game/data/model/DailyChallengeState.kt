package com.mergeseven.game.data.model

/**
 * Data model representing the Daily Challenge status and requirements for today.
 */
data class DailyChallengeState(
    val dateSeed: String = "",
    val title: String = "DAILY CHALLENGE",
    val targetScore: Int = 3000,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val coinsReward: Int = 500,
    val starsReward: Int = 5,
    val attempts: Int = 0
)
