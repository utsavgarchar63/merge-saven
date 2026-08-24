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
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private var soundPlaceId: Int = 0
    private var soundMergeId: Int = 0
    private var soundComboId: Int = 0

    var isMusicEnabled: Boolean = true
        private set
    var isSoundEnabled: Boolean = true
        private set

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
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSoundPlace() {
        if (!isSoundEnabled || soundPlaceId == 0) return
        soundPool?.play(soundPlaceId, 0.8f, 0.8f, 1, 0, 1.0f)
    }

    fun playSoundMerge() {
        if (!isSoundEnabled || soundMergeId == 0) return
        soundPool?.play(soundMergeId, 0.9f, 0.9f, 2, 0, 1.0f)
    }

    fun playSoundCombo() {
        if (!isSoundEnabled || soundComboId == 0) return
        soundPool?.play(soundComboId, 1.0f, 1.0f, 3, 0, 1.0f)
    }

    fun release() {
        stopMusic()
        soundPool?.release()
        soundPool = null
    }
}
