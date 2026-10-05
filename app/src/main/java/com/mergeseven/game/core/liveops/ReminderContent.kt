package com.mergeseven.game.core.liveops

import com.mergeseven.game.data.model.DailyGifts
import com.mergeseven.game.data.model.UserProfile
import java.time.LocalDate

data class ReminderContent(val title: String, val body: String, val action: String)

object DailyReminderContent {
    /** Preview today's gift without recording a login or changing offline progression. */
    fun giftCoins(profile: UserProfile, today: LocalDate): Int? {
        val last = runCatching { LocalDate.parse(profile.lastLoginDate) }.getOrNull()
        if (last != null && last.isAfter(today)) return null
        if (DailyGifts.claimKey(today.toString()) in profile.rewardClaims) return null
        if (last == today) return DailyGifts.available(DailyGifts.migrate(profile))?.coins
        val day = if (last == today.minusDays(1) && 7 !in profile.claimedDays)
            DailyGifts.rewards.firstOrNull { it.day !in profile.claimedDays }?.day ?: 1 else 1
        return DailyGifts.rewards.first { it.day == day }.coins
    }

    fun forPlayer(profile: UserProfile, today: LocalDate): ReminderContent? {
        val gift = giftCoins(profile, today)
        if (gift != null) return ReminderContent("Your next hexagon adventure starts with a gift",
            "$gift free coins are waiting. Claim your daily gift, then build a satisfying merge chain.", "Claim gift")
        val quest = profile.dailyQuests.firstOrNull { it.isCompleted && !it.isClaimed }
            ?.takeIf { profile.lastLoginDate == today.toString() }
        if (quest != null) return ReminderContent("Nice work — your quest reward is ready",
            "Collect ${quest.coinsReward} earned coins and keep your next merge going.", "Collect reward")
        if (profile.dailyChallenge.dateSeed == today.toString() && profile.dailyChallenge.isCompleted) return null
        return ReminderContent("A fresh puzzle, a satisfying chain",
            "Take a quiet moment with today's Merge Seven challenge. Your next clever merge is waiting.", "Play puzzle")
    }
}
