package com.mergeseven.game.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.ads.AdPlacement
import com.mergeseven.game.ads.AdPreloader
import com.mergeseven.game.ads.AdResult
import com.mergeseven.game.ads.AdService
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.model.BoosterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.mergeseven.game.meta.AchievementMetric
import com.mergeseven.game.meta.AchievementTracker

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
    private val audioManager: AudioManager,
    private val featureFlags: FeatureFlags,
    private val boosterInventory: BoosterInventoryStore,
    private val analyticsTracker: AnalyticsTracker,
    private val adService: AdService,
    private val adPreloader: AdPreloader,
    private val achievementTracker: AchievementTracker
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userDataRepository.userProfile
    val af9Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF9, viewModelScope)

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    init {
        refreshDailyCheck()
    }

    fun refreshDailyCheck() {
        val today = runCatching { LocalDate.now().format(DateTimeFormatter.ISO_DATE) }
            .getOrDefault("")
        if (today.isNotEmpty()) {
            userDataRepository.checkDailyLogin(today)
            viewModelScope.launch {
                achievementTracker.reportMetric(
                    AchievementMetric.STREAK,
                    userDataRepository.userProfile.value.currentStreak
                )
            }
        }
    }

    fun getDailyRewards(): List<DailyRewardItem> {
        val profile = userProfile.value
        val streak = profile.currentStreak
        val claimed = profile.claimedDays

        val rewardConfigs = listOf(
            Pair(50, 0),
            Pair(100, 0),
            Pair(150, 2),
            Pair(200, 0),
            Pair(300, 3),
            Pair(500, 5),
            Pair(1000, 10)
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

    val adAvailability = adService.availability
    private val _adBusy = MutableStateFlow(false)
    val adBusy = _adBusy.asStateFlow()
    fun extraClaimed() = userDataRepository.rewardClaimed("${userProfile.value.dailyChallenge.dateSeed}:extra_daily")
    fun warmAds() { adPreloader.warmDaily() }
    fun watchAdForExtraAttempt(activity: android.app.Activity) {
        val key = "${userProfile.value.dailyChallenge.dateSeed}:extra_daily"
        if (!featureFlags.isEnabled(Feature.AF9) || _adBusy.value || extraClaimed()) return
        if (!adService.isReady(AdPlacement.EXTRA_DAILY)) {
            warmAds(); _status.value = "No ad available right now. You can keep playing."; return
        }
        _adBusy.value = true
        viewModelScope.launch {
            try {
                val result = adService.showRewarded(activity, AdPlacement.EXTRA_DAILY) {
                    userDataRepository.claimReward(key, 0, extraDailyAttempt = true)
                }
                _status.value = if (result == AdResult.Rewarded) "Bonus retry unlocked. Your official score is kept." else "No reward claimed"
            } finally { _adBusy.value = false }
        }
    }

    fun claimQuest(questId: String) {
        val before = userProfile.value.dailyQuests.firstOrNull { it.id == questId }
        userDataRepository.claimQuestReward(questId)
        val after = userProfile.value.dailyQuests.firstOrNull { it.id == questId }
        if (before != null && after != null && !before.isClaimed && after.isClaimed) {
            audioManager.playSoundCombo()
            if (featureFlags.isEnabled(Feature.AF5)) {
                userDataRepository.addXp(Constants.XP_QUEST_CLAIM)
                analyticsTracker.logEvent(AnalyticsEvents.XP_GAINED, mapOf("source" to "quest"))
            }
            if (featureFlags.isEnabled(Feature.AF3)) {
                viewModelScope.launch {
                    val type = when (questId) {
                        "quest_merge" -> BoosterType.UNDO
                        "quest_level" -> BoosterType.RANDOMIZE
                        "quest_score" -> BoosterType.REMOVE
                        else -> BoosterType.UNDO
                    }
                    boosterInventory.grant(type, 1)
                }
            }
        }
    }
}
