package com.mergeseven.game.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.competitive.LeaderboardEntry
import com.mergeseven.game.competitive.LeaderboardId
import com.mergeseven.game.competitive.LeaderboardRepository
import com.mergeseven.game.competitive.LeaderboardScope
import com.mergeseven.game.cloud.PlayGamesAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val board: LeaderboardId = LeaderboardId.ENDLESS,
    val scope: LeaderboardScope = LeaderboardScope.GLOBAL,
    val entries: List<LeaderboardEntry> = emptyList(),
    val loading: Boolean = false,
    val offlineSoft: Boolean = false,
    val signedIn: Boolean = false,
    val enabled: Boolean = false
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val auth: PlayGamesAuth,
    featureFlags: FeatureFlags
) : ViewModel() {

    val enabled: StateFlow<Boolean> =
        featureFlags.enabledState(Feature.AF8, viewModelScope)

    private val _ui = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            enabled.collect { on ->
                _ui.value = _ui.value.copy(enabled = on)
                if (on) refresh()
            }
        }
        viewModelScope.launch {
            auth.account.collect { account ->
                _ui.value = _ui.value.copy(signedIn = account != null)
            }
        }
    }

    fun selectBoard(board: LeaderboardId) {
        _ui.value = _ui.value.copy(board = board)
        refresh()
    }

    fun selectScope(scope: LeaderboardScope) {
        _ui.value = _ui.value.copy(scope = scope)
        refresh()
    }

    fun refresh() {
        val state = _ui.value
        if (!state.enabled) return
        viewModelScope.launch {
            val cached = repository.cachedScores(state.board, state.scope)
            _ui.value = state.copy(loading = true, entries = cached, offlineSoft = false)
            val loaded = repository.loadScores(state.board, state.scope)
            _ui.value = _ui.value.copy(
                loading = false,
                entries = loaded.ifEmpty { cached },
                offlineSoft = loaded.isEmpty() && cached.isEmpty()
            )
        }
    }
}
