package com.mergeseven.game.data.model

/**
 * Everything the player accumulates outside of a single run: wallet, stars, and daily-cycle state.
 *
 * The defaults here are also the first-run seed values written by
 * [com.mergeseven.game.data.local.PersistenceSeeder].
 */
data class UserProfile(
    val coins: Int = STARTING_COINS,
    val totalStars: Int = 0,
    val currentStreak: Int = 1,
    val claimedDays: Set<Int> = emptySet(),
    val lastLoginDate: String = "",
    val dailyQuests: List<DailyQuest> = defaultDailyQuests(),
    val dailyChallenge: DailyChallengeState = DailyChallengeState(),
    /** AF5-01 */
    val xp: Int = 0,
    val playerLevel: Int = 1,
    /** AF5-06 equipped theme ids */
    val equippedTileThemeId: String = "classic",
    val equippedBoardThemeId: String = "wood",
    /** AF5-07 lifetime stats */
    val totalMerges: Int = 0,
    val biggestTile: Int = 0,
    val longestChain: Int = 0,
    val playtimeMs: Long = 0L
) {
    companion object {
        const val STARTING_COINS = 250
    }
}
