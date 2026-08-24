package com.mergeseven.game.ui.settings

import android.content.Context
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var audioManager: AudioManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        userDataRepository = UserDataRepository()

        val dummyContext = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java)
        ) { _, _, _ -> null } as Context

        audioManager = AudioManager(dummyContext)
        viewModel = SettingsViewModel(fakeSettingsRepository, userDataRepository, audioManager)
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

    private class FakeSettingsRepository : SettingsRepository {
        val soundFlow = MutableStateFlow(true)
        val musicFlow = MutableStateFlow(true)
        val hapticsFlow = MutableStateFlow(true)
        val notificationsFlow = MutableStateFlow(true)
        val tutorialFlow = MutableStateFlow(false)

        override val isSoundEnabled: Flow<Boolean> = soundFlow
        override val isMusicEnabled: Flow<Boolean> = musicFlow
        override val isHapticsEnabled: Flow<Boolean> = hapticsFlow
        override val isNotificationsEnabled: Flow<Boolean> = notificationsFlow
        override val isTutorialCompleted: Flow<Boolean> = tutorialFlow

        override suspend fun setSoundEnabled(enabled: Boolean) {
            soundFlow.value = enabled
        }

        override suspend fun setMusicEnabled(enabled: Boolean) {
            musicFlow.value = enabled
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            hapticsFlow.value = enabled
        }

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            notificationsFlow.value = enabled
        }

        override suspend fun setTutorialCompleted(completed: Boolean) {
            tutorialFlow.value = completed
        }

        override suspend fun resetSettings() {
            soundFlow.value = true
            musicFlow.value = true
            hapticsFlow.value = true
            notificationsFlow.value = true
            tutorialFlow.value = false
        }
    }
}
