package com.mergeseven.game.data.repository

import com.mergeseven.game.data.model.DailyChallengeState
import com.mergeseven.game.data.model.DailyQuest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val coins: Int = 250,
    val totalStars: Int = 0,
    val currentStreak: Int = 1,
    val claimedDays: Set<Int> = emptySet<Int>(),
    val lastLoginDate: String = "",
    val dailyQuests: List<DailyQuest> = defaultDailyQuests(),
    val dailyChallenge: DailyChallengeState = DailyChallengeState()
)

private fun defaultDailyQuests(): List<DailyQuest> = listOf(
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

/**
 * Singleton repository for persisting user coins, total stars, daily streak, quests, and challenges.
 */
@Singleton
class UserDataRepository @Inject constructor() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        checkDailyLogin(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
    }

    fun addCoins(amount: Int) {
        _userProfile.update { it.copy(coins = it.coins + amount) }
    }

    fun addStars(amount: Int) {
        _userProfile.update { it.copy(totalStars = it.totalStars + amount) }
    }

    fun checkDailyLogin(todayDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)) {
        _userProfile.update { profile ->
            if (profile.lastLoginDate.isEmpty()) {
                profile.copy(
                    lastLoginDate = todayDate,
                    dailyChallenge = profile.dailyChallenge.copy(dateSeed = todayDate)
                )
            } else if (profile.lastLoginDate != todayDate) {
                val lastDate = runCatching { LocalDate.parse(profile.lastLoginDate) }.getOrNull()
                val today = runCatching { LocalDate.parse(todayDate) }.getOrNull()

                val isConsecutive = lastDate != null && today != null && lastDate.plusDays(1) == today

                val newStreak = if (isConsecutive) {
                    if (profile.claimedDays.contains(7)) 1 else profile.currentStreak
                } else {
                    1
                }

                val newClaimed = if (isConsecutive && !profile.claimedDays.contains(7)) profile.claimedDays else emptySet()

                profile.copy(
                    lastLoginDate = todayDate,
                    currentStreak = newStreak,
                    claimedDays = newClaimed,
                    dailyQuests = defaultDailyQuests(),
                    dailyChallenge = DailyChallengeState(dateSeed = todayDate)
                )
            } else {
                if (profile.dailyChallenge.dateSeed.isEmpty()) {
                    profile.copy(dailyChallenge = profile.dailyChallenge.copy(dateSeed = todayDate))
                } else {
                    profile
                }
            }
        }
    }

    fun claimDailyReward(day: Int, coinsReward: Int, starsReward: Int) {
        _userProfile.update { profile ->
            val nextStreak = if (day >= profile.currentStreak && day < 7) day + 1 else profile.currentStreak
            profile.copy(
                coins = profile.coins + coinsReward,
                totalStars = profile.totalStars + starsReward,
                claimedDays = profile.claimedDays + day,
                currentStreak = nextStreak
            )
        }
    }

    fun updateQuestProgress(questId: String, progressIncrement: Int) {
        _userProfile.update { profile ->
            val updatedQuests = profile.dailyQuests.map { quest ->
                if (quest.id == questId && !quest.isClaimed) {
                    val newProgress = (quest.currentProgress + progressIncrement).coerceAtMost(quest.targetProgress)
                    quest.copy(currentProgress = newProgress)
                } else {
                    quest
                }
            }
            profile.copy(dailyQuests = updatedQuests)
        }
    }

    fun claimQuestReward(questId: String) {
        _userProfile.update { profile ->
            var coinsToAdd = 0
            var starsToAdd = 0

            val updatedQuests = profile.dailyQuests.map { quest ->
                if (quest.id == questId && quest.isCompleted && !quest.isClaimed) {
                    coinsToAdd = quest.coinsReward
                    starsToAdd = quest.starsReward
                    quest.copy(isClaimed = true)
                } else {
                    quest
                }
            }

            profile.copy(
                coins = profile.coins + coinsToAdd,
                totalStars = profile.totalStars + starsToAdd,
                dailyQuests = updatedQuests
            )
        }
    }

    fun completeDailyChallenge(score: Int) {
        _userProfile.update { profile ->
            val currentChallenge = profile.dailyChallenge
            val newBest = maxOf(currentChallenge.bestScore, score)
            val isCompleted = currentChallenge.isCompleted || score >= currentChallenge.targetScore
            val isFirstTimeCompletion = isCompleted && !currentChallenge.isCompleted

            val bonusCoins = if (isFirstTimeCompletion) currentChallenge.coinsReward else 0
            val bonusStars = if (isFirstTimeCompletion) currentChallenge.starsReward else 0

            profile.copy(
                coins = profile.coins + bonusCoins,
                totalStars = profile.totalStars + bonusStars,
                dailyChallenge = currentChallenge.copy(
                    bestScore = newBest,
                    isCompleted = isCompleted,
                    attempts = currentChallenge.attempts + 1
                )
            )
        }
    }
}

