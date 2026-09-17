package com.mergeseven.game.ui.feel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.haptics.HapticManager
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.HexCoord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class DropSnapRequest(
    val id: Long,
    val from: Offset,
    val toCells: List<Pair<Offset, Color>>,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class JuiceUiState(
    val reduceMotion: Boolean = false,
    val shakeAmplitudePx: Float = 0f,
    val hitStopRemainingMs: Long = 0L,
    val comboLength: Int = 0,
    val showComboBanner: Boolean = false,
    val slowMoScale: Float = 1f,
    val musicIntensity: Float = 0f,
    val dropSnap: DropSnapRequest? = null,
    val confettiWidth: Float = 0f,
    val pendingBurstCells: List<HexCoord> = emptyList(),
    val pendingSparkPairs: List<Pair<HexCoord, HexCoord>> = emptyList(),
    val pendingConfetti: Boolean = false,
    val revision: Long = 0L
)

/**
 * AF10 juice hub: maps [GameEvent]s to presentation state + audio/haptics.
 */
@Singleton
class JuiceController @Inject constructor(
    private val audioManager: AudioManager,
    private val hapticManager: HapticManager,
    private val settingsRepository: SettingsRepository
) {
    val particles = ParticleSystem()
    val frameBudget = FrameBudgetGuard()

    private val _uiState = MutableStateFlow(JuiceUiState())
    val uiState: StateFlow<JuiceUiState> = _uiState.asStateFlow()

    @Volatile
    var af10Enabled: Boolean = false

    private var scope: CoroutineScope? = null
    private var decayJob: Job? = null
    private var dropIdSeq = 0L

    fun bind(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            settingsRepository.isReduceMotionEnabled.collect { enabled ->
                setReduceMotion(enabled)
            }
        }
        scope.launch {
            settingsRepository.isHapticsEnabled.collect { enabled ->
                hapticManager.isEnabled = enabled
            }
        }
    }

    fun setReduceMotion(enabled: Boolean) {
        _uiState.update { it.copy(reduceMotion = enabled) }
    }

    fun onLegacyAudio(result: GameResult) {
        val hasMerge = result.events.any { it is GameEvent.MergeCompleted }
        val hasCombo = result.events.any { it is GameEvent.ChainCompleted }
        audioManager.playSoundPlace()
        if (hasCombo) {
            audioManager.playSoundCombo()
        } else if (hasMerge) {
            audioManager.playSoundMerge()
        }
    }

    fun dispatch(result: GameResult, dropFrom: Offset? = null, dropTargets: List<Pair<Offset, Color>> = emptyList()) {
        if (!af10Enabled) {
            onLegacyAudio(result)
            return
        }

        val reduceMotion = _uiState.value.reduceMotion
        var shake = 0f
        var hitStop = 0L
        var comboLen = 0
        var slowMo = 1f
        var musicIntensity = _uiState.value.musicIntensity
        val bursts = mutableListOf<HexCoord>()
        val sparks = mutableListOf<Pair<HexCoord, HexCoord>>()
        var confetti = false
        var dropSnap: DropSnapRequest? = null

        audioManager.playSoundPlace(pitch = 1f)
        hapticManager.playPlace()

        if (!reduceMotion && dropFrom != null && dropTargets.isNotEmpty()) {
            dropSnap = DropSnapRequest(
                id = ++dropIdSeq,
                from = dropFrom,
                toCells = dropTargets
            )
        }

        var mergeIndex = 0
        for (event in result.events) {
            when (event) {
                is GameEvent.MergeStarted -> {
                    if (!reduceMotion) {
                        sparks += event.sourceTiles.map { it.cell to event.destinationCoord }
                    }
                }
                is GameEvent.MergeCompleted -> {
                    mergeIndex++
                    val pitch = pitchForChain(mergeIndex)
                    audioManager.playSoundMerge(pitch)
                    hapticManager.playMerge()
                    if (!reduceMotion) {
                        bursts += event.resultTile.cell
                        val t = (event.mergedCount / 8f).coerceIn(0.25f, 1f)
                        shake = maxOf(shake, Constants.AF10_MAX_SHAKE_PX * t)
                        hitStop = maxOf(
                            hitStop,
                            (Constants.AF10_MAX_HIT_STOP_MS * t).toLong()
                        )
                    }
                }
                is GameEvent.ChainCompleted -> {
                    comboLen = event.chainLength
                    val pitch = pitchForChain(event.chainLength)
                    audioManager.playSoundCombo(pitch)
                    hapticManager.playChain()
                    musicIntensity = intensityForChain(event.chainLength)
                    if (!reduceMotion && event.chainLength >= Constants.AF10_SLOW_MO_CHAIN_MIN) {
                        slowMo = Constants.AF10_SLOW_MO_SCALE
                    }
                }
                is GameEvent.LevelCompleted -> {
                    if (!reduceMotion) confetti = true
                    audioManager.playSoundCombo(1.2f)
                }
                is GameEvent.InvalidPlacement -> hapticManager.playFail()
                is GameEvent.GameOver -> hapticManager.playFail()
                else -> Unit
            }
        }

        audioManager.setMusicIntensity(musicIntensity)

        _uiState.update {
            it.copy(
                shakeAmplitudePx = if (reduceMotion) 0f else shake,
                hitStopRemainingMs = if (reduceMotion) 0L else hitStop,
                comboLength = comboLen,
                showComboBanner = comboLen > 1,
                slowMoScale = if (reduceMotion) 1f else slowMo,
                musicIntensity = musicIntensity,
                dropSnap = dropSnap,
                pendingBurstCells = bursts,
                pendingSparkPairs = sparks,
                pendingConfetti = confetti,
                revision = it.revision + 1
            )
        }

        scheduleDecay(shake > 0f || hitStop > 0L || slowMo < 1f || comboLen > 1 || musicIntensity > 0f)
    }

    fun onInvalidPlacement() {
        if (!af10Enabled) return
        hapticManager.playFail()
    }

    fun consumePendingEmits() {
        _uiState.update {
            it.copy(
                pendingBurstCells = emptyList(),
                pendingSparkPairs = emptyList(),
                pendingConfetti = false
            )
        }
    }

    fun clearDropSnap(id: Long) {
        _uiState.update { state ->
            if (state.dropSnap?.id == id) state.copy(dropSnap = null) else state
        }
    }

    fun tickFeelClocks(dtMs: Float): Float {
        val state = _uiState.value
        var scale = state.slowMoScale
        if (state.hitStopRemainingMs > 0) {
            scale = 0f
            val remaining = (state.hitStopRemainingMs - dtMs.toLong()).coerceAtLeast(0L)
            if (remaining != state.hitStopRemainingMs) {
                _uiState.update { it.copy(hitStopRemainingMs = remaining) }
            }
        }
        return scale
    }

    fun setConfettiWidth(width: Float) {
        if (width > 0f && width != _uiState.value.confettiWidth) {
            _uiState.update { it.copy(confettiWidth = width) }
        }
    }

    private fun scheduleDecay(needed: Boolean) {
        if (!needed) return
        val jobScope = scope ?: return
        decayJob?.cancel()
        decayJob = jobScope.launch {
            val start = System.currentTimeMillis()
            val shakeStart = _uiState.value.shakeAmplitudePx
            val musicStart = _uiState.value.musicIntensity
            while (true) {
                delay(16L)
                val elapsed = System.currentTimeMillis() - start
                val shakeT = (elapsed / Constants.AF10_SHAKE_DECAY_MS.toFloat()).coerceIn(0f, 1f)
                val bannerDone = elapsed >= Constants.AF10_COMBO_BANNER_MS
                val slowDone = elapsed >= Constants.AF10_SLOW_MO_MS
                val musicT = (elapsed / Constants.AF10_MUSIC_INTENSITY_DECAY_MS.toFloat()).coerceIn(0f, 1f)
                val shake = shakeStart * (1f - shakeT)
                val intensity = musicStart * (1f - musicT)
                audioManager.setMusicIntensity(intensity)
                _uiState.update {
                    it.copy(
                        shakeAmplitudePx = shake,
                        showComboBanner = it.showComboBanner && !bannerDone,
                        slowMoScale = if (slowDone) 1f else it.slowMoScale,
                        musicIntensity = intensity
                    )
                }
                if (shakeT >= 1f && bannerDone && slowDone && musicT >= 1f) break
            }
        }
    }

    fun pitchForChain(chainLength: Int): Float {
        val t = ((chainLength - 1).coerceAtLeast(0) / 5f).coerceIn(0f, 1f)
        return Constants.AF10_SFX_PITCH_MIN +
            (Constants.AF10_SFX_PITCH_MAX - Constants.AF10_SFX_PITCH_MIN) * t
    }

    fun intensityForChain(chainLength: Int): Float =
        min(1f, (chainLength - 1).coerceAtLeast(0) / 4f)

    fun resetSession() {
        particles.clear()
        frameBudget.reset()
        audioManager.setMusicIntensity(0f)
        _uiState.value = JuiceUiState(reduceMotion = _uiState.value.reduceMotion)
    }
}
