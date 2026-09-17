package com.mergeseven.game.ui.feel

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.haptics.HapticManager
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.testing.fakeContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParticleSystemTest {

    @Test
    fun poolAcquireReleaseKeepsBudget() {
        val system = ParticleSystem(poolSize = 32)
        system.emitBurst(0f, 0f, count = 20, color = Color.Red, maxActive = 16)
        assertEquals(16, system.activeCount)
        assertEquals(16, system.freeCount)
        system.clear()
        assertEquals(0, system.activeCount)
        assertEquals(32, system.freeCount)
    }

    @Test
    fun tickExpiresParticles() {
        val system = ParticleSystem(poolSize = 8)
        system.emitBurst(0f, 0f, count = 4, color = Color.White, maxActive = 8)
        repeat(40) { system.tick(0.1f) }
        assertEquals(0, system.activeCount)
    }
}

class FrameBudgetGuardTest {

    @Test
    fun overBudgetDropsTierAndCap() {
        val guard = FrameBudgetGuard(budgetMs = 16f)
        assertEquals(Constants.AF10_PARTICLE_CAP_FULL, guard.activeParticleCap())
        repeat(8) { guard.recordFrameMs(30f) }
        assertTrue(guard.tier.ordinal >= FrameBudgetGuard.QualityTier.HALF.ordinal)
        assertTrue(guard.activeParticleCap() < Constants.AF10_PARTICLE_CAP_FULL)
    }

    @Test
    fun resetRestoresFullTier() {
        val guard = FrameBudgetGuard()
        repeat(10) { guard.recordFrameMs(40f) }
        guard.reset()
        assertEquals(FrameBudgetGuard.QualityTier.FULL, guard.tier)
        assertEquals(Constants.AF10_PARTICLE_CAP_FULL, guard.activeParticleCap())
    }
}

class JuiceControllerTest {

    private lateinit var juice: JuiceController

    @Before
    fun setup() {
        juice = JuiceController(
            audioManager = AudioManager(fakeContext()),
            hapticManager = HapticManager(fakeContext()),
            settingsRepository = FakeSettings()
        )
        juice.af10Enabled = true
    }

    @Test
    fun pitchScalesWithChainLength() {
        assertTrue(juice.pitchForChain(1) < juice.pitchForChain(6))
        assertEquals(Constants.AF10_SFX_PITCH_MIN, juice.pitchForChain(1), 0.001f)
        assertEquals(Constants.AF10_SFX_PITCH_MAX, juice.pitchForChain(6), 0.001f)
    }

    @Test
    fun mergeEventsRaiseShakeAndBurstWhenMotionAllowed() {
        val tile = Tile.normal(1L, 8, HexCoord(0, 0))
        juice.dispatch(
            GameResult(
                state = emptyState(),
                events = listOf(
                    GameEvent.MergeCompleted(resultTile = tile, mergedCount = 4, scoreEarned = 40L),
                    GameEvent.ChainCompleted(chainLength = 4, totalScoreEarned = 100L)
                )
            )
        )
        val state = juice.uiState.value
        assertTrue(state.shakeAmplitudePx > 0f)
        assertTrue(state.shakeAmplitudePx <= Constants.AF10_MAX_SHAKE_PX)
        assertTrue(state.showComboBanner)
        assertEquals(Constants.AF10_SLOW_MO_SCALE, state.slowMoScale, 0.001f)
        assertEquals(listOf(HexCoord(0, 0)), state.pendingBurstCells)
    }

    @Test
    fun reduceMotionSkipsShakeParticlesAndSlowMo() {
        juice.setReduceMotion(true)
        val tile = Tile.normal(1L, 8, HexCoord(0, 0))
        juice.dispatch(
            GameResult(
                state = emptyState(),
                events = listOf(
                    GameEvent.MergeCompleted(tile, 5, 50L),
                    GameEvent.ChainCompleted(5, 120L),
                    GameEvent.LevelCompleted(level = 1, score = 100L, maxTileValue = 16)
                )
            )
        )
        val state = juice.uiState.value
        assertEquals(0f, state.shakeAmplitudePx, 0.001f)
        assertEquals(1f, state.slowMoScale, 0.001f)
        assertTrue(state.pendingBurstCells.isEmpty())
        assertFalse(state.pendingConfetti)
        assertTrue(state.showComboBanner)
    }

    private fun emptyState(): GameState = GameState(
        board = com.mergeseven.game.game.engine.BoardEngine().createBoard(),
        trayPieces = listOf(null, null, null),
        score = 0,
        bestScore = 0,
        coins = 0,
        level = 1,
        targetValue = 16
    )

    private class FakeSettings : SettingsRepository {
        override val isSoundEnabled = MutableStateFlow(true)
        override val isMusicEnabled = MutableStateFlow(true)
        override val isHapticsEnabled = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val isNotificationsEnabled = MutableStateFlow(true)
        override val isTutorialCompleted = MutableStateFlow(false)
        override val colourblindMode = MutableStateFlow(ColourblindMode.OFF)
        override val largeTouchTargets = MutableStateFlow(false)
        override suspend fun setSoundEnabled(enabled: Boolean) = Unit
        override suspend fun setMusicEnabled(enabled: Boolean) = Unit
        override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
        override suspend fun setReduceMotionEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setTutorialCompleted(completed: Boolean) = Unit
        override suspend fun setColourblindMode(mode: ColourblindMode) = Unit
        override suspend fun setLargeTouchTargets(enabled: Boolean) = Unit
        override suspend fun resetSettings() = Unit
    }
}
