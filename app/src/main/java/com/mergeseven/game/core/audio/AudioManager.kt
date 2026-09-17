package com.mergeseven.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.mergeseven.game.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Game Audio Manager for Background Music and Sound Effects.
 * Uses MediaPlayer for continuous ambient background music loop
 * and SoundPool for instant low-latency game sound effects.
 * AF10-05 adds intensity stems that crossfade with combo heat.
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var midPlayer: MediaPlayer? = null
    private var highPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private var soundPlaceId: Int = 0
    private var soundMergeId: Int = 0
    private var soundComboId: Int = 0

    var isMusicEnabled: Boolean = true
        private set
    var isSoundEnabled: Boolean = true
        private set

    @Volatile
    private var musicIntensity: Float = 0f

    init {
        runCatching {
            initSoundPool()
            initMusicPlayer()
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        if (enabled) {
            startMusic()
        } else {
            pauseMusic()
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    fun setMusicIntensity(intensity: Float) {
        musicIntensity = intensity.coerceIn(0f, 1f)
        applyLayerVolumes()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            soundPlaceId = pool.load(context, R.raw.sound_place, 1)
            soundMergeId = pool.load(context, R.raw.sound_merge, 1)
            soundComboId = pool.load(context, R.raw.sound_combo, 1)
        }
    }

    private fun initMusicPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.bg_music)?.apply {
                isLooping = true
                setVolume(0.4f, 0.4f)
            }
            midPlayer = MediaPlayer.create(context, R.raw.bg_music_mid)?.apply {
                isLooping = true
                setVolume(0f, 0f)
            }
            highPlayer = MediaPlayer.create(context, R.raw.bg_music_high)?.apply {
                isLooping = true
                setVolume(0f, 0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startMusic() {
        if (!isMusicEnabled) return
        try {
            if (mediaPlayer == null) {
                initMusicPlayer()
            }
            listOf(mediaPlayer, midPlayer, highPlayer).forEach { player ->
                if (player?.isPlaying == false) {
                    player.start()
                }
            }
            applyLayerVolumes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseMusic() {
        try {
            listOf(mediaPlayer, midPlayer, highPlayer).forEach { player ->
                if (player?.isPlaying == true) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMusic() {
        try {
            listOf(mediaPlayer, midPlayer, highPlayer).forEach { player ->
                player?.stop()
                player?.release()
            }
            mediaPlayer = null
            midPlayer = null
            highPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSoundPlace(pitch: Float = 1.0f) {
        if (!isSoundEnabled || soundPlaceId == 0) return
        soundPool?.play(soundPlaceId, 0.8f, 0.8f, 1, 0, pitch.coerceIn(0.5f, 2.0f))
    }

    fun playSoundMerge(pitch: Float = 1.0f) {
        if (!isSoundEnabled || soundMergeId == 0) return
        soundPool?.play(soundMergeId, 0.9f, 0.9f, 2, 0, pitch.coerceIn(0.5f, 2.0f))
    }

    fun playSoundCombo(pitch: Float = 1.0f) {
        if (!isSoundEnabled || soundComboId == 0) return
        soundPool?.play(soundComboId, 1.0f, 1.0f, 3, 0, pitch.coerceIn(0.5f, 2.0f))
    }

    private fun applyLayerVolumes() {
        if (!isMusicEnabled) return
        val intensity = musicIntensity
        val base = 0.4f * (1f - intensity * 0.35f)
        val mid = (intensity * 0.55f).coerceIn(0f, 0.55f)
        val high = ((intensity - 0.45f) / 0.55f).coerceIn(0f, 1f) * 0.5f
        runCatching {
            mediaPlayer?.setVolume(base, base)
            midPlayer?.setVolume(mid, mid)
            highPlayer?.setVolume(high, high)
        }
    }

    fun release() {
        stopMusic()
        soundPool?.release()
        soundPool = null
    }
}
