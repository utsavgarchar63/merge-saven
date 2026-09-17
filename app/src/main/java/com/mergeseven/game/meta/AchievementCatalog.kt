package com.mergeseven.game.meta

enum class AchievementMetric {
    MERGES,
    BIGGEST_TILE,
    LEVELS_CLEARED,
    STREAK,
    SCORE
}

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val metric: AchievementMetric,
    /** Cosmetic granted when completed, if any. */
    val cosmeticRewardId: String? = null
)

object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_merge", "First Merge", "Complete your first merge", 1, AchievementMetric.MERGES),
        AchievementDef("merges_100", "Merger", "Complete 100 merges", 100, AchievementMetric.MERGES),
        AchievementDef("merges_500", "Merge Master", "Complete 500 merges", 500, AchievementMetric.MERGES),
        AchievementDef("tile_64", "Hex Climber", "Create a 64 tile", 64, AchievementMetric.BIGGEST_TILE),
        AchievementDef("tile_128", "Power Tile", "Create a 128 tile", 128, AchievementMetric.BIGGEST_TILE),
        AchievementDef(
            "levels_5",
            "Campaigner",
            "Clear 5 campaign levels",
            5,
            AchievementMetric.LEVELS_CLEARED,
            cosmeticRewardId = "tile_neon"
        ),
        AchievementDef("streak_7", "Week Warrior", "Reach a 7-day streak", 7, AchievementMetric.STREAK),
        AchievementDef("score_5k", "High Roller", "Score 5,000 in a run", 5_000, AchievementMetric.SCORE)
    )

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }

    fun unlockId(id: String): String = "ach_$id"
}
