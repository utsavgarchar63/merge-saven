package com.mergeseven.game.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AF10-06: composed VibrationEffect patterns for gameplay feedback.
 */
@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    @Volatile
    var isEnabled: Boolean = true

    fun playPlace() = vibrate(PLACE)

    fun playMerge() = vibrate(MERGE)

    fun playChain() = vibrate(CHAIN)

    fun playFail() = vibrate(FAIL)

    private fun vibrate(effect: VibrationEffect?) {
        if (!isEnabled || effect == null) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !v.hasVibrator()) return
        runCatching { v.vibrate(effect) }
    }

    companion object {
        private val PLACE: VibrationEffect? = createOneShot(18, 80)

        private val MERGE: VibrationEffect? = createWaveform(
            timings = longArrayOf(0, 22, 30, 28),
            amplitudes = intArrayOf(0, 120, 0, 160)
        )

        private val CHAIN: VibrationEffect? = createWaveform(
            timings = longArrayOf(0, 18, 24, 18, 24, 36),
            amplitudes = intArrayOf(0, 100, 0, 140, 0, 200)
        )

        private val FAIL: VibrationEffect? = createWaveform(
            timings = longArrayOf(0, 40, 40, 55),
            amplitudes = intArrayOf(0, 180, 0, 90)
        )

        private fun createOneShot(ms: Long, amplitude: Int): VibrationEffect? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255))
            } else {
                null
            }

        private fun createWaveform(timings: LongArray, amplitudes: IntArray): VibrationEffect? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                null
            }
    }
}
