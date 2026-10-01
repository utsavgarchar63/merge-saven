package com.mergeseven.game.ui.feel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.remember
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
): State<Offset> {
    val offset = remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(amplitudePx, reduceMotion) {
        if (reduceMotion || amplitudePx <= 0.1f) {
            offset.value = Offset.Zero
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
                    var ox = (Random.nextFloat() - 0.5f) * 2f * mag
                    val oy = (Random.nextFloat() - 0.5f) * 2f * mag
                    // Slight sinusoidal bias so it doesn't look pure noise
                    ox += sin(elapsed / 16f) * mag * 0.25f
                    offset.value = Offset(ox, oy)
                }
                last = nanos
            }
        }
        offset.value = Offset.Zero
    }
    return offset
}

fun Modifier.shakeGraphics(offset: State<Offset>): Modifier =
    graphicsLayer {
        translationX = offset.value.x
        translationY = offset.value.y
    }
