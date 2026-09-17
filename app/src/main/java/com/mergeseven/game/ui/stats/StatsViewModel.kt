package com.mergeseven.game.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.data.local.store.ModeRecordsStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.data.repository.LevelRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.modes.ModeIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeStatRow(val title: String, val bestScore: Long, val runs: Int)

data class StatsUiState(
    val profile: UserProfile = UserProfile(),
    val modeRows: List<ModeStatRow> = emptyList(),
    val campaignStars: Int = 0,
    val af5Enabled: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
    private val modeRecordsStore: ModeRecordsStore,
    private val levelRepository: LevelRepository,
    featureFlags: FeatureFlags
) : ViewModel() {

    init {
        viewModelScope.launch { modeRecordsStore.load() }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        userDataRepository.userProfile,
        modeRecordsStore.records,
        featureFlags.enabledState(Feature.AF5, viewModelScope)
    ) { profile, records, af5 ->
        val rows = listOf(
            ModeIds.CAMPAIGN to "Campaign",
            ModeIds.ENDLESS to "Endless",
            ModeIds.TIME_ATTACK to "Time Attack",
            ModeIds.ZEN to "Zen",
            ModeIds.DAILY to "Daily",
            ModeIds.WEEKLY to "Weekly"
        ).map { (id, title) ->
            val rec = records[id]
            ModeStatRow(title, rec?.bestScore ?: 0L, rec?.runs ?: 0)
        }
        StatsUiState(
            profile = profile,
            modeRows = rows,
            campaignStars = levelRepository.totalStars(),
            af5Enabled = af5
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
