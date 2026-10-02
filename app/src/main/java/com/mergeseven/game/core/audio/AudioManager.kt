package com.mergeseven.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.mergeseven.game.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** One original ambient loop; asynchronously prepared music and low-latency sound effects. */
@Singleton
class AudioManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: MediaPlayer? = null
    private var pool: SoundPool? = null
    private var prepared = false
    private var appForeground = false
    private var gameplayPaused = false
    private var focusLost = false
    private var ducked = false
    private var ownsFocus = false
    private var intensity = 0f
    private var volume = 0f
    private val loaded = mutableSetOf<Int>()
    private var placeId = 0
    private var mergeId = 0
    private var comboId = 0
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val audioSystem by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager }
    private val attributes by lazy {
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    }
    private val focusListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            android.media.AudioManager.AUDIOFOCUS_GAIN -> { focusLost = false; ducked = false; startMusic() }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; fadeTo(targetVolume()) }
            else -> { focusLost = true; if (change == android.media.AudioManager.AUDIOFOCUS_LOSS) ownsFocus = false; pausePlayer() }
        }
    }
    private val focusRequest by lazy {
        if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener).build() else null
    }
    var isMusicEnabled = true
        private set
    var isSoundEnabled = true
        private set

    init {
        runCatching {
            pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build().apply {
                setOnLoadCompleteListener { _, sample, status -> if (status == 0) loaded += sample }
                placeId = load(context, R.raw.sound_place, 1)
                mergeId = load(context, R.raw.sound_merge, 1)
                comboId = load(context, R.raw.sound_combo, 1)
            }
        }
    }
    fun setAppForeground(foreground: Boolean) { appForeground = foreground; if (!foreground) pauseMusic() }
    fun setGameplayPaused(paused: Boolean) { gameplayPaused = paused; if (paused) pauseMusic() else startMusic() }
    fun setMusicEnabled(enabled: Boolean) { isMusicEnabled = enabled; if (enabled) startMusic() else pauseMusic() }
    fun setSoundEnabled(enabled: Boolean) { isSoundEnabled = enabled }
    fun setMusicIntensity(value: Float) { intensity = value.coerceIn(0f, 1f); if (canPlay() && prepared) fadeTo(targetVolume()) }
    private fun canPlay() = isMusicEnabled && appForeground && !gameplayPaused && !focusLost
    private fun targetVolume() = (.45f + .12f * intensity) * if (ducked) .2f else 1f
    @Suppress("DEPRECATION")
    private fun acquireFocus(): Boolean {
        if (ownsFocus) return true
        val manager = audioSystem ?: return false
        val result = if (Build.VERSION.SDK_INT >= 26) manager.requestAudioFocus(requireNotNull(focusRequest))
        else manager.requestAudioFocus(focusListener, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)
        ownsFocus = result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return ownsFocus
    }
    fun startMusic() {
        if (!canPlay()) return
        runCatching {
            if (!acquireFocus()) return
            if (player == null) {
                player = MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    context.resources.openRawResourceFd(R.raw.bg_music).use { fd -> setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
                    isLooping = true; setVolume(0f, 0f)
                    setOnPreparedListener { prepared = true; startMusic() }
                    setOnErrorListener { _, _, _ -> stopMusic(); true }
                    prepareAsync()
                }
            }
            if (prepared) { if (player?.isPlaying == false) player?.start(); fadeTo(targetVolume()) }
        }.onFailure { stopMusic() }
    }
    private var fade: Runnable? = null
    private var fadeTarget = -1f
    private fun fadeTo(target: Float) {
        if (!prepared || (fade != null && target == fadeTarget)) return
        if (fade == null && kotlin.math.abs(volume - target) < .005f) return
        fade?.let(handler::removeCallbacks)
        fadeTarget = target
        val start = volume
        var step = 0
        val task = object : Runnable {
            override fun run() {
                step++
                volume = start + (target - start) * (step / 10f).coerceAtMost(1f)
                runCatching { player?.setVolume(volume, volume) }
                if (step < 10 && prepared) handler.postDelayed(this, 20) else fade = null
            }
        }
        fade = task; handler.post(task)
    }
    private fun pausePlayer() {
        fade?.let(handler::removeCallbacks); fade = null; volume = 0f
        runCatching { if (prepared && player?.isPlaying == true) player?.pause(); player?.setVolume(0f,0f) }
    }
    @Suppress("DEPRECATION")
    fun pauseMusic() {
        pausePlayer()
        if (ownsFocus) {
            if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audioSystem?.abandonAudioFocusRequest(it) }
            else audioSystem?.abandonAudioFocus(focusListener)
            ownsFocus = false
        }
        focusLost = false
    }
    fun stopMusic() { pauseMusic(); runCatching { player?.release() }; player = null; prepared = false }
    private fun sound(id: Int, volume: Float, priority: Int, pitch: Float) {
        if (!isSoundEnabled || !appForeground || gameplayPaused || focusLost || id !in loaded) return
        pool?.play(id, volume, volume, priority, 0, pitch.coerceIn(.5f, 2f))
    }
    fun playSoundPlace(pitch: Float = 1f) = sound(placeId, .6f, 1, pitch)
    fun playSoundMerge(pitch: Float = 1f) = sound(mergeId, .7f, 2, pitch)
    fun playSoundCombo(pitch: Float = 1f) = sound(comboId, .8f, 3, pitch)
    fun release() { stopMusic(); pool?.release(); pool = null; loaded.clear() }
}
