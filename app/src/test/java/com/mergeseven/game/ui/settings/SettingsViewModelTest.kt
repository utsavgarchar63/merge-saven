package com.mergeseven.game.ui.settings

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
            pushTopicManager = com.mergeseven.game.core.liveops.PushTopicManager(
                settingsRepository = fakeSettingsRepository,
                featureFlags = featureFlags,
                scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
            ),
            playGamesAuth = com.mergeseven.game.cloud.FakePlayGamesAuth(),
            cloudSyncCoordinator = com.mergeseven.game.cloud.FakeCloudStack.coordinator(
                featureFlags = featureFlags,
                scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
            ),
            accountDataExporter = com.mergeseven.game.cloud.AccountDataExporter(
                context = fakeContext(),
                builder = com.mergeseven.game.cloud.CloudSnapshotBuilder(
                    userProfileStore = com.mergeseven.game.data.local.store.InMemoryUserProfileStore(),
                    levelProgressStore = com.mergeseven.game.data.local.store.InMemoryLevelProgressStore(),
                    unlockDao = com.mergeseven.game.testing.InMemoryUnlockDao(),
                    deviceIdStore = com.mergeseven.game.cloud.FixedDeviceIdStore("test")
                )
            ),
            accountDeletionService = com.mergeseven.game.cloud.AccountDeletionService(
                applier = com.mergeseven.game.cloud.CloudSnapshotApplier(
                    userDataRepository = userDataRepository,
                    levelProgressStore = com.mergeseven.game.data.local.store.InMemoryLevelProgressStore(),
                    levelProgressDao = object : com.mergeseven.game.data.local.dao.LevelProgressDao {
                        override fun observeAll() =
                            kotlinx.coroutines.flow.flowOf(emptyList<com.mergeseven.game.data.local.entity.LevelProgressEntity>())
                        override suspend fun getAll() =
                            emptyList<com.mergeseven.game.data.local.entity.LevelProgressEntity>()
                        override suspend fun upsert(entity: com.mergeseven.game.data.local.entity.LevelProgressEntity) = Unit
                        override suspend fun totalStars() = 0
                        override suspend fun highestCompletedLevel() = 0
                        override suspend fun clear() = Unit
                    },
                    unlockDao = com.mergeseven.game.testing.InMemoryUnlockDao()
                ),
                cloudSave = com.mergeseven.game.cloud.InMemoryCloudSaveRepository(),
                syncQueue = com.mergeseven.game.cloud.InMemorySyncQueue(),
                auth = com.mergeseven.game.cloud.FakePlayGamesAuth(),
                activeGameDao = object : com.mergeseven.game.data.local.dao.ActiveGameDao {
                    override fun observe(slotId: String) = kotlinx.coroutines.flow.flowOf(null)
                    override suspend fun get(slotId: String) = null
                    override suspend fun upsert(entity: com.mergeseven.game.data.local.entity.ActiveGameEntity) = Unit
                    override suspend fun delete(slotId: String) = Unit
                },
                scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
            ),
            billingRepository = com.mergeseven.game.billing.FakeBillingRepository()
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
        assertTrue(state.isNotificationsEnabled)
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
        assertEquals(750, userDataRepository.userProfile.value.coins)

        viewModel.confirmResetData()
        assertFalse(viewModel.uiState.value.showResetDialog)
        assertEquals(250, userDataRepository.userProfile.value.coins)
    }

    @Test
    fun `grant debug coins bumps the wallet`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val before = userDataRepository.userProfile.value.coins
        viewModel.grantDebugCoins(1_000)
        assertEquals(before + 1_000, userDataRepository.userProfile.value.coins)
    }

    @Test
    fun `force game over without a session reports open a game first`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onVersionLongPressed()
        viewModel.forceGameOver()

        assertEquals("Open a game first", viewModel.uiState.value.debugStatusMessage)
    }

    @Test
    fun `toggle reduce motion updates settings`() = runTest {
        viewModel.toggleReduceMotion(true)
        assertTrue(fakeSettingsRepository.reduceMotionFlow.value)
    }

    @Test
    fun `toggling a feature flag updates the snapshot`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setFeatureEnabled(Feature.AF1, true)

        assertTrue(featureFlags.isEnabled(Feature.AF1))
        assertTrue(viewModel.uiState.value.featureFlags[Feature.AF1] == true)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val soundFlow = MutableStateFlow(true)
        val musicFlow = MutableStateFlow(true)
        val hapticsFlow = MutableStateFlow(true)
        val reduceMotionFlow = MutableStateFlow(false)
        val notificationsFlow = MutableStateFlow(true)
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
            notificationsFlow.value = true
            tutorialFlow.value = false
            colourblindFlow.value = com.mergeseven.game.data.preferences.ColourblindMode.OFF
            largeTouchFlow.value = false
        }
    }
}
