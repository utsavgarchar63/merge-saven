package com.mergeseven.game.ui.daily

import androidx.lifecycle.ViewModel
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.data.model.DailyQuest
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.data.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyRewardItem(
    val day: Int,
    val coins: Int,
    val stars: Int,
    val isClaimed: Boolean,
    val isAvailable: Boolean
)

@HiltViewModel
class DailyViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val audioManager: AudioManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userDataRepository.userProfile

    init {
        refreshDailyCheck()
    }

    fun refreshDailyCheck() {
        val today = runCatching { LocalDate.now().format(DateTimeFormatter.ISO_DATE) }
            .getOrDefault("")
        if (today.isNotEmpty()) {
            userDataRepository.checkDailyLogin(today)
        }
    }

    fun getDailyRewards(): List<DailyRewardItem> {
        val profile = userProfile.value
        val streak = profile.currentStreak
        val claimed = profile.claimedDays

        val rewardConfigs = listOf(
            Pair(50, 0),    // Day 1
            Pair(100, 0),   // Day 2
            Pair(150, 2),   // Day 3
            Pair(200, 0),   // Day 4
            Pair(300, 3),   // Day 5
            Pair(500, 5),   // Day 6
            Pair(1000, 10)  // Day 7 Jackpot
        )

        return rewardConfigs.mapIndexed { index, (coins, stars) ->
            val day = index + 1
            DailyRewardItem(
                day = day,
                coins = coins,
                stars = stars,
                isClaimed = day in claimed,
                isAvailable = day <= streak && day !in claimed
            )
        }
    }

    fun claimReward(item: DailyRewardItem) {
        if (item.isAvailable && !item.isClaimed) {
            userDataRepository.claimDailyReward(item.day, item.coins, item.stars)
            audioManager.playSoundCombo()
        }
    }

    fun claimQuest(questId: String) {
        userDataRepository.claimQuestReward(questId)
        audioManager.playSoundCombo()
    }
}

