package com.mergeseven.game.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.data.repository.LevelItem
import com.mergeseven.game.data.repository.LevelRepository
import com.mergeseven.game.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelsUiState(
    val levels: List<LevelItem> = emptyList(),
    val selectedLevel: LevelItem? = null,
    val totalStars: Int = 0,
    val highestUnlockedLevel: Int = 1,
    val coins: Int = 250
)

@HiltViewModel
class LevelsViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelsUiState())
    val uiState: StateFlow<LevelsUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
        viewModelScope.launch {
            levelRepository.highestUnlockedLevel.collect {
                loadLevels()
            }
        }
        viewModelScope.launch {
            userDataRepository.userProfile.collect { profile ->
                _uiState.update { state ->
                    state.copy(
                        coins = profile.coins,
                        totalStars = profile.totalStars + levelRepository.totalStars(30)
                    )
                }
            }
        }
    }

    fun loadLevels() {
        val levelList = levelRepository.getLevels(30)
        val profile = userDataRepository.userProfile.value
        val stars = levelRepository.totalStars(30) + profile.totalStars
        _uiState.update { state ->
            state.copy(
                levels = levelList,
                totalStars = stars,
                coins = profile.coins
            )
        }
    }

    fun onSelectLevel(levelItem: LevelItem) {
        if (levelItem.isUnlocked) {
            _uiState.update { it.copy(selectedLevel = levelItem) }
        }
    }

    fun dismissDetailModal() {
        _uiState.update { it.copy(selectedLevel = null) }
    }
}
