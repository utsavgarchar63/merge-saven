package com.mergeseven.game.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.core.liveops.ActiveSeasonalEvent
import com.mergeseven.game.core.liveops.SeasonalEventStore
import com.mergeseven.game.core.liveops.SeasonalRewardStep
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.meta.AchievementProgress
import com.mergeseven.game.meta.AchievementTracker
import com.mergeseven.game.meta.MilestoneCatalog
import com.mergeseven.game.meta.MilestoneDef
import com.mergeseven.game.meta.UnlockService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MilestoneRow(
    val def: MilestoneDef,
    val eligible: Boolean,
    val claimed: Boolean
)

data class SeasonalRewardRow(
    val step: SeasonalRewardStep,
    val eligible: Boolean,
    val claimed: Boolean
)

data class AchievementsUiState(
    val achievements: List<AchievementProgress> = emptyList(),
    val milestones: List<MilestoneRow> = emptyList(),
    val seasonal: ActiveSeasonalEvent? = null,
    val seasonalRewards: List<SeasonalRewardRow> = emptyList(),
    val toastTitle: String? = null,
    val af5Enabled: Boolean = false
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementTracker: AchievementTracker,
    private val unlockService: UnlockService,
    private val userDataRepository: UserDataRepository,
    private val seasonalEventStore: SeasonalEventStore,
    featureFlags: FeatureFlags
) : ViewModel() {

    init {
        viewModelScope.launch {
            achievementTracker.load()
            unlockService.loadAndSeed()
        }
    }

    private val baseUi = combine(
        achievementTracker.progress,
        achievementTracker.newlyCompleted,
        unlockService.claimedMilestones,
        userDataRepository.userProfile,
        featureFlags.enabledState(Feature.AF5, viewModelScope)
    ) { _, toast, claimed, profile, af5 ->
        AchievementsUiState(
            achievements = achievementTracker.snapshot(),
            milestones = MilestoneCatalog.all.map { def ->
                MilestoneRow(
                    def = def,
                    eligible = unlockService.isMilestoneEligible(
                        def,
                        profile.totalMerges,
                        profile.biggestTile
                    ),
                    claimed = def.id in claimed
                )
            },
            toastTitle = toast?.title,
            af5Enabled = af5
        )
    }

    val uiState: StateFlow<AchievementsUiState> = combine(
        baseUi,
        seasonalEventStore.active
    ) { base, seasonal ->
        base.copy(
            seasonal = seasonal,
            seasonalRewards = seasonal?.config?.rewardTrack?.map { step ->
                SeasonalRewardRow(
                    step = step,
                    eligible = seasonal.personalBest >= step.threshold &&
                        step.threshold !in seasonal.claimedThresholds,
                    claimed = step.threshold in seasonal.claimedThresholds
                )
            }.orEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())

    fun dismissToast() = achievementTracker.consumeToast()

    fun claimMilestone(id: String) {
        val def = MilestoneCatalog.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { unlockService.claimMilestone(def) }
    }

    fun claimSeasonal(threshold: Long) {
        viewModelScope.launch { seasonalEventStore.claim(threshold) }
    }
}
