package com.mergeseven.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isNotificationsEnabled: Boolean = true,
    val appVersion: String = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val showResetDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userDataRepository: UserDataRepository,
    private val audioManager: AudioManager
) : ViewModel() {

    private val _showResetDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.isSoundEnabled,
        settingsRepository.isMusicEnabled,
        settingsRepository.isHapticsEnabled,
        settingsRepository.isNotificationsEnabled,
        _showResetDialog
    ) { sound, music, haptics, notifications, showReset ->
        audioManager.setSoundEnabled(sound)
        audioManager.setMusicEnabled(music)

        SettingsUiState(
            isSoundEnabled = sound,
            isMusicEnabled = music,
            isHapticsEnabled = haptics,
            isNotificationsEnabled = notifications,
            appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            showResetDialog = showReset
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
            audioManager.setSoundEnabled(enabled)
        }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMusicEnabled(enabled)
            audioManager.setMusicEnabled(enabled)
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun onResetClicked() {
        _showResetDialog.update { true }
    }

    fun dismissResetDialog() {
        _showResetDialog.update { false }
    }

    fun confirmResetData() {
        viewModelScope.launch {
            _showResetDialog.update { false }
            settingsRepository.resetSettings()
            userDataRepository.addCoins(-userDataRepository.userProfile.value.coins + 250)
            userDataRepository.addStars(-userDataRepository.userProfile.value.totalStars)
            audioManager.playSoundCombo()
        }
    }
}
