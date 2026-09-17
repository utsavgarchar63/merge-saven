package com.mergeseven.game.ui.feel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import kotlin.random.Random

/**
 * AF10-02: decaying board shake driven by [JuiceUiState.shakeAmplitudePx].
 */
@Composable
fun rememberShakeOffset(
    amplitudePx: Float,
    reduceMotion: Boolean
): Pair<Float, Float> {
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(amplitudePx, reduceMotion) {
        if (reduceMotion || amplitudePx <= 0.1f) {
            ox = 0f
            oy = 0f
            return@LaunchedEffect
        }
        var elapsed = 0f
        var last = 0L
        while (elapsed < 320f) {
            withFrameNanos { nanos ->
                if (last != 0L) {
                    val dt = (nanos - last) / 1_000_000f
                    elapsed += dt
                    val decay = (1f - elapsed / 320f).coerceIn(0f, 1f)
                    val mag = amplitudePx * decay
                    ox = (Random.nextFloat() - 0.5f) * 2f * mag
                    oy = (Random.nextFloat() - 0.5f) * 2f * mag
                    // Slight sinusoidal bias so it doesn't look pure noise
                    ox += sin(elapsed / 16f) * mag * 0.25f
                }
                last = nanos
            }
        }
        ox = 0f
        oy = 0f
    }
    return ox to oy
}

fun Modifier.shakeGraphics(offsetX: Float, offsetY: Float): Modifier =
    graphicsLayer {
        translationX = offsetX
        translationY = offsetY
    }
