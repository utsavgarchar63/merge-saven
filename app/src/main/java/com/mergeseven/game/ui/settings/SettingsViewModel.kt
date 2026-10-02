package com.mergeseven.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.debug.DebugCommands
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LocalReminderManager
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.LevelRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.solver.DifficultyId
import com.mergeseven.game.game.solver.DifficultyProfileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isReduceMotionEnabled: Boolean = false,
    val isNotificationsEnabled: Boolean = false,
    val colourblindMode: ColourblindMode = ColourblindMode.OFF,
    val largeTouchTargets: Boolean = false,
    val af11Enabled: Boolean = false,
    val appVersion: String = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val showResetDialog: Boolean = false,
    val showDebugMenu: Boolean = false,
    val debugMenuAvailable: Boolean = BuildConfig.DEBUG,
    val featureFlags: Map<Feature, Boolean> = Feature.entries.associateWith { false },
    val unlockLevelInput: String = "5",
    val debugStatusMessage: String? = null,
    val difficultyProfile: String = "STANDARD",
    val af9Enabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userDataRepository: UserDataRepository,
    private val audioManager: AudioManager,
    private val featureFlags: FeatureFlags,
    private val levelRepository: LevelRepository,
    private val debugCommands: DebugCommands,
    private val difficultyProfileProvider: DifficultyProfileProvider,
    private val analyticsTracker: AnalyticsTracker,
    private val reminderManager: LocalReminderManager,
    private val consentManager: com.mergeseven.game.ads.ConsentManager = com.mergeseven.game.ads.FakeConsentManager(false)
) : ViewModel() {

    val privacyOptionsRequired = consentManager.privacyOptionsRequired
    fun showPrivacyOptions(activity: android.app.Activity) { viewModelScope.launch { consentManager.showPrivacyOptions(activity) } }
    fun replayTutorial() { viewModelScope.launch { settingsRepository.resetTutorialGuide() } }

    private val _showResetDialog = MutableStateFlow(false)
    private val _showDebugMenu = MutableStateFlow(false)
    private val _unlockLevelInput = MutableStateFlow("5")
    private val _debugStatusMessage = MutableStateFlow<String?>(null)
    private val flagSnapshot: StateFlow<Map<Feature, Boolean>> =
        combine(Feature.entries.map { feature -> featureFlags.observe(feature).map { feature to it } }) { pairs ->
            pairs.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = featureFlags.snapshot()
        )

    private val settingsPart = combine(
        combine(
            settingsRepository.isSoundEnabled,
            settingsRepository.isMusicEnabled,
            settingsRepository.isHapticsEnabled,
            settingsRepository.isReduceMotionEnabled,
            settingsRepository.isNotificationsEnabled
        ) { sound, music, haptics, reduceMotion, notifications ->
            audioManager.setSoundEnabled(sound)
            audioManager.setMusicEnabled(music)
            SettingsAudio(sound, music, haptics, reduceMotion, notifications)
        },
        combine(
            _showResetDialog,
            settingsRepository.colourblindMode,
            settingsRepository.largeTouchTargets
        ) { showReset, colourblind, largeTouch ->
            Triple(showReset, colourblind, largeTouch)
        }
    ) { audio, extras ->
        SettingsPart(
            sound = audio.sound,
            music = audio.music,
            haptics = audio.haptics,
            reduceMotion = audio.reduceMotion,
            notifications = audio.notifications,
            showReset = extras.first,
            colourblindMode = extras.second,
            largeTouchTargets = extras.third
        )
    }

    private val debugPart = combine(
        _showDebugMenu,
        flagSnapshot,
        _unlockLevelInput,
        _debugStatusMessage,
        difficultyProfileProvider.profile
    ) { showDebug, flags, unlockInput, status, profile ->
        DebugPart(showDebug, flags, unlockInput, status, profile.id.name)
    }

    val uiState: StateFlow<SettingsUiState> = combine(settingsPart, debugPart) { settings, debug ->
        SettingsUiState(
            isSoundEnabled = settings.sound,
            isMusicEnabled = settings.music,
            isHapticsEnabled = settings.haptics,
            isReduceMotionEnabled = settings.reduceMotion,
            isNotificationsEnabled = settings.notifications,
            colourblindMode = settings.colourblindMode,
            largeTouchTargets = settings.largeTouchTargets,
            af11Enabled = debug.flags[Feature.AF11] == true,
            appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            showResetDialog = settings.showReset,
            showDebugMenu = debug.showDebug && BuildConfig.DEBUG,
            debugMenuAvailable = BuildConfig.DEBUG,
            featureFlags = debug.flags,
            unlockLevelInput = debug.unlockInput,
            debugStatusMessage = debug.status,
            difficultyProfile = debug.difficultyProfile,
            af9Enabled = debug.flags[Feature.AF9] == true
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

    fun toggleReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceMotionEnabled(enabled)
            analyticsTracker.logEvent(
                AnalyticsEvents.SETTINGS_CHANGED,
                mapOf("setting" to "reduce_motion", "value" to enabled.toString())
            )
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            analyticsTracker.logEvent(
                AnalyticsEvents.SETTINGS_CHANGED,
                mapOf("setting" to "notifications", "value" to enabled.toString())
            )
            reminderManager.sync()
        }
    }

    fun setColourblindMode(mode: ColourblindMode) {
        viewModelScope.launch {
            settingsRepository.setColourblindMode(mode)
            analyticsTracker.logEvent(
                AnalyticsEvents.SETTINGS_CHANGED,
                mapOf("setting" to "colourblind_mode", "value" to mode.name)
            )
        }
    }

    fun toggleLargeTouchTargets(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLargeTouchTargets(enabled)
            analyticsTracker.logEvent(
                AnalyticsEvents.SETTINGS_CHANGED,
                mapOf("setting" to "large_touch_targets", "value" to enabled.toString())
            )
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
            userDataRepository.addCoins(-userDataRepository.userProfile.value.coins + 100)
            userDataRepository.addStars(-userDataRepository.userProfile.value.totalStars)
            audioManager.playSoundCombo()
        }
    }

    fun onVersionLongPressed() {
        if (!BuildConfig.DEBUG) return
        _debugStatusMessage.value = null
        _showDebugMenu.value = true
    }

    fun dismissDebugMenu() {
        _showDebugMenu.value = false
        _debugStatusMessage.value = null
    }

    fun grantDebugCoins(amount: Int = 1_000) {
        if (!BuildConfig.DEBUG) return
        userDataRepository.addCoins(amount)
        _debugStatusMessage.value = "Granted +$amount coins"
    }

    fun onUnlockLevelInputChange(value: String) {
        _unlockLevelInput.value = value.filter { it.isDigit() }.take(3)
    }

    fun unlockThroughLevel() {
        if (!BuildConfig.DEBUG) return
        val level = _unlockLevelInput.value.toIntOrNull()
        if (level == null || level < 1) {
            _debugStatusMessage.value = "Enter a level >= 1"
            return
        }
        levelRepository.unlockThrough(level)
        _debugStatusMessage.value = "Unlocked through level $level"
    }

    fun forceGameOver() {
        if (!BuildConfig.DEBUG) return
        val accepted = debugCommands.forceGameOver()
        _debugStatusMessage.value = if (accepted) {
            "Forced game over"
        } else {
            "Open a game first"
        }
    }

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            featureFlags.setEnabled(feature, enabled)

        }
    }

    fun setDifficultyProfile(idName: String) {
        if (!BuildConfig.DEBUG) return
        val id = runCatching { DifficultyId.valueOf(idName) }.getOrNull() ?: return
        viewModelScope.launch {
            difficultyProfileProvider.setProfile(id)
            _debugStatusMessage.value = "Difficulty: ${id.name}"
        }
    }

    private data class SettingsAudio(
        val sound: Boolean,
        val music: Boolean,
        val haptics: Boolean,
        val reduceMotion: Boolean,
        val notifications: Boolean
    )

    private data class SettingsPart(
        val sound: Boolean,
        val music: Boolean,
        val haptics: Boolean,
        val reduceMotion: Boolean,
        val notifications: Boolean,
        val showReset: Boolean,
        val colourblindMode: ColourblindMode,
        val largeTouchTargets: Boolean
    )

    private data class DebugPart(
        val showDebug: Boolean,
        val flags: Map<Feature, Boolean>,
        val unlockInput: String,
        val status: String?,
        val difficultyProfile: String
    )


}
