package com.mergeseven.game.data.model

/** Free gifts advance after a claim, with the next gift available on a later local date. */
object DailyGifts {
    data class Gift(val day: Int, val coins: Int, val stars: Int)
    val rewards = listOf(50 to 0, 100 to 0, 150 to 2, 200 to 0, 300 to 3, 500 to 5, 1000 to 10)
        .mapIndexed { index, reward -> Gift(index + 1, reward.first, reward.second) }
    const val MIGRATION_KEY = "daily_gifts:v2"
    fun claimKey(date: String) = "$date:daily_gift"

    fun available(profile: UserProfile): Gift? {
        if (profile.lastLoginDate.isEmpty() || claimKey(profile.lastLoginDate) in profile.rewardClaims) return null
        return rewards.firstOrNull { it.day !in profile.claimedDays }
            ?.takeIf { it.day <= profile.currentStreak }
    }

    /** Preserve old balances/claims and conservatively prevent another gift on upgrade day. */
    fun migrate(profile: UserProfile): UserProfile {
        if (MIGRATION_KEY in profile.rewardClaims) return profile
        val previousClaim = if (profile.claimedDays.isNotEmpty() && profile.lastLoginDate.isNotEmpty())
            mapOf(claimKey(profile.lastLoginDate) to 0) else emptyMap()
        return profile.copy(rewardClaims = profile.rewardClaims + previousClaim + (MIGRATION_KEY to 0))
    }
}
