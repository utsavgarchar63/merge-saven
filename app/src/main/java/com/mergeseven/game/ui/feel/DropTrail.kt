package com.mergeseven.game.ui.feel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mergeseven.game.core.Constants

@Composable
fun DropTrailOverlay(
    request: DropSnapRequest?,
    reduceMotion: Boolean,
    onFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (request == null || reduceMotion) return

    val progress = remember(request.id) { Animatable(0f) }
    LaunchedEffect(request.id) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
        )
        onFinished(request.id)
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        for ((target, color) in request.toCells) {
            val x = request.from.x + (target.x - request.from.x) * t
            val y = request.from.y + (target.y - request.from.y) * t
            for (i in 1..Constants.AF10_DROP_TRAIL_SAMPLES) {
                val trailT = (t - i * 0.06f).coerceAtLeast(0f)
                val tx = request.from.x + (target.x - request.from.x) * trailT
                val ty = request.from.y + (target.y - request.from.y) * trailT
                drawCircle(
                    color = color.copy(alpha = 0.18f * (1f - i / Constants.AF10_DROP_TRAIL_SAMPLES.toFloat())),
                    radius = 14f,
                    center = Offset(tx, ty)
                )
            }
            val hexSize = 28f
            val pathAlpha = (1f - t * 0.35f).coerceIn(0.4f, 1f)
            drawCircle(
                color = color.copy(alpha = pathAlpha),
                radius = hexSize * 0.85f,
                center = Offset(x, y)
            )
            if (t > 0.85f) {
                val flash = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * (1f - flash)),
                    radius = hexSize * (1f + flash),
                    center = target
                )
            }
        }
    }
}
