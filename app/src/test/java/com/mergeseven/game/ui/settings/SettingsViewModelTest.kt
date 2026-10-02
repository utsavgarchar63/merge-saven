package com.mergeseven.game.ui.settings

import com.mergeseven.game.BuildConfig
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.debug.DebugCommands
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.testing.TestPersistence
import com.mergeseven.game.testing.fakeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var audioManager: AudioManager
    private lateinit var featureFlags: InMemoryFeatureFlags
    private lateinit var debugCommands: DebugCommands
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        userDataRepository = TestPersistence.userDataRepository()
        featureFlags = InMemoryFeatureFlags(isDebug = true)
        debugCommands = DebugCommands()

        audioManager = AudioManager(fakeContext())
        viewModel = SettingsViewModel(
            settingsRepository = fakeSettingsRepository,
            userDataRepository = userDataRepository,
            audioManager = audioManager,
            featureFlags = featureFlags,
            levelRepository = TestPersistence.levelRepository(),
            debugCommands = debugCommands,
            difficultyProfileProvider = object : com.mergeseven.game.game.solver.DifficultyProfileProvider {
                private val flow = MutableStateFlow(com.mergeseven.game.game.solver.DifficultyProfiles.STANDARD)
                override val profile = flow
                override suspend fun setProfile(id: com.mergeseven.game.game.solver.DifficultyId) {
                    flow.value = com.mergeseven.game.game.solver.DifficultyProfiles.of(id)
                }
            },
            analyticsTracker = com.mergeseven.game.core.analytics.NoOpAnalyticsTracker(),
            reminderManager = com.mergeseven.game.core.liveops.LocalReminderManager(fakeContext(), fakeSettingsRepository, kotlinx.coroutines.CoroutineScope(testDispatcher))

        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state values`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.isSoundEnabled)
        assertTrue(state.isMusicEnabled)
        assertTrue(state.isHapticsEnabled)
        assertFalse(state.isNotificationsEnabled)
        assertFalse(state.showResetDialog)
        assertFalse(state.showDebugMenu)
    }

    @Test
    fun `test toggle sound updates settings`() = runTest {
        viewModel.toggleSound(false)
        assertFalse(fakeSettingsRepository.soundFlow.value)
        assertFalse(audioManager.isSoundEnabled)
    }

    @Test
    fun `test toggle music updates settings`() = runTest {
        viewModel.toggleMusic(false)
        assertFalse(fakeSettingsRepository.musicFlow.value)
        assertFalse(audioManager.isMusicEnabled)
    }

    @Test
    fun `test reset dialog open dismiss and confirm`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertFalse(viewModel.uiState.value.showResetDialog)

        viewModel.onResetClicked()
        assertTrue(viewModel.uiState.value.showResetDialog)

        viewModel.dismissResetDialog()
        assertFalse(viewModel.uiState.value.showResetDialog)

        viewModel.onResetClicked()
        userDataRepository.addCoins(500)
        assertEquals(600, userDataRepository.userProfile.value.coins)

        viewModel.confirmResetData()
        assertFalse(viewModel.uiState.value.showResetDialog)
        assertEquals(100, userDataRepository.userProfile.value.coins)
    }

    @Test
    fun `debug coin grants respect the build variant`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val before = userDataRepository.userProfile.value.coins
        viewModel.grantDebugCoins(1_000)
        assertEquals(if (BuildConfig.DEBUG) before + 1_000 else before,
            userDataRepository.userProfile.value.coins)
        if (!BuildConfig.DEBUG) assertEquals(null, viewModel.uiState.value.debugStatusMessage)
    }

    @Test
    fun `debug game over and menu respect the build variant`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onVersionLongPressed()
        viewModel.forceGameOver()

        assertEquals(if (BuildConfig.DEBUG) "Open a game first" else null,
            viewModel.uiState.value.debugStatusMessage)
        assertEquals(BuildConfig.DEBUG, viewModel.uiState.value.showDebugMenu)
    }

    @Test
    fun `toggle reduce motion updates settings`() = runTest {
        viewModel.toggleReduceMotion(true)
        assertTrue(fakeSettingsRepository.reduceMotionFlow.value)
    }

    @Test
    fun `debug feature overrides respect the build variant`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val before = featureFlags.isEnabled(Feature.AF1)
        val snapshotBefore = viewModel.uiState.value.featureFlags
        viewModel.setFeatureEnabled(Feature.AF1, true)
        assertEquals(if (BuildConfig.DEBUG) true else before, featureFlags.isEnabled(Feature.AF1))
        if (BuildConfig.DEBUG) assertTrue(viewModel.uiState.value.featureFlags[Feature.AF1] == true)
        else assertEquals(snapshotBefore, viewModel.uiState.value.featureFlags)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val soundFlow = MutableStateFlow(true)
        val musicFlow = MutableStateFlow(true)
        val hapticsFlow = MutableStateFlow(true)
        val reduceMotionFlow = MutableStateFlow(false)
        val notificationsFlow = MutableStateFlow(false)
        val tutorialFlow = MutableStateFlow(false)
        val colourblindFlow = MutableStateFlow(com.mergeseven.game.data.preferences.ColourblindMode.OFF)
        val largeTouchFlow = MutableStateFlow(false)

        override val isSoundEnabled: Flow<Boolean> = soundFlow
        override val isMusicEnabled: Flow<Boolean> = musicFlow
        override val isHapticsEnabled: Flow<Boolean> = hapticsFlow
        override val isReduceMotionEnabled: Flow<Boolean> = reduceMotionFlow
        override val isNotificationsEnabled: Flow<Boolean> = notificationsFlow
        override val isTutorialCompleted: Flow<Boolean> = tutorialFlow
        override val colourblindMode: Flow<com.mergeseven.game.data.preferences.ColourblindMode> = colourblindFlow
        override val largeTouchTargets: Flow<Boolean> = largeTouchFlow

        override suspend fun setSoundEnabled(enabled: Boolean) {
            soundFlow.value = enabled
        }

        override suspend fun setMusicEnabled(enabled: Boolean) {
            musicFlow.value = enabled
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            hapticsFlow.value = enabled
        }

        override suspend fun setReduceMotionEnabled(enabled: Boolean) {
            reduceMotionFlow.value = enabled
        }

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            notificationsFlow.value = enabled
        }

        override suspend fun setTutorialCompleted(completed: Boolean) {
            tutorialFlow.value = completed
        }

        override suspend fun setColourblindMode(mode: com.mergeseven.game.data.preferences.ColourblindMode) {
            colourblindFlow.value = mode
        }

        override suspend fun setLargeTouchTargets(enabled: Boolean) {
            largeTouchFlow.value = enabled
        }

        override suspend fun resetSettings() {
            soundFlow.value = true
            musicFlow.value = true
            hapticsFlow.value = true
            reduceMotionFlow.value = false
            notificationsFlow.value = false
            tutorialFlow.value = false
            colourblindFlow.value = com.mergeseven.game.data.preferences.ColourblindMode.OFF
            largeTouchFlow.value = false
        }
    }
}
