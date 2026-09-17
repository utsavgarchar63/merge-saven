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

/**
 * The quest set handed out at the start of each day.
 *
 * Lives here rather than in the repository because both the repository and the first-run seeder
 * need it, and because AF6 will replace this with a Remote Config-driven list.
 */
fun defaultDailyQuests(): List<DailyQuest> = listOf(
    DailyQuest(
        id = "quest_merge",
        title = "Merge 10 Tiles Today",
        description = "Merge any 10 tiles during gameplay",
        currentProgress = 0,
        targetProgress = 10,
        coinsReward = 100,
        starsReward = 1
    ),
    DailyQuest(
        id = "quest_level",
        title = "Reach Level 3 Target",
        description = "Clear target score or complete a level",
        currentProgress = 0,
        targetProgress = 1,
        coinsReward = 200,
        starsReward = 2
    ),
    DailyQuest(
        id = "quest_score",
        title = "Achieve 2,000 Score",
        description = "Reach 2,000 points in a single session",
        currentProgress = 0,
        targetProgress = 2000,
        coinsReward = 300,
        starsReward = 3
    )
)
