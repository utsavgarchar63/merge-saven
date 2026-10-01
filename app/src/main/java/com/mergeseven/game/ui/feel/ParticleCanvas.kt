package com.mergeseven.game.ui.feel

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun ParticleCanvas(
    juice: JuiceController,
    boardRadius: Int,
    modifier: Modifier = Modifier
) {
    var frame by remember { mutableIntStateOf(0) }
    val ui by juice.uiState.collectAsStateWithLifecycle()
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(ui.revision, boardSize, ui.reduceMotion, lifecycle) {
        if (ui.reduceMotion) {
            juice.particles.clear()
            frame++
            juice.consumePendingEmits()
            return@LaunchedEffect
        }
        if (boardSize.width == 0) {
            return@LaunchedEffect
        }
        if (ui.pendingBurstCells.isEmpty() &&
            ui.pendingSparkPairs.isEmpty() &&
            !ui.pendingConfetti && juice.particles.activeCount == 0
        ) {
            return@LaunchedEffect
        }

        val width = boardSize.width.toFloat()
        val height = boardSize.height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val hexSize = HexGeometry.calculateHexSize(boardRadius, width, height, 8f)
        // An idle clock has no samples with which to recover from a previous OFF tier.
        if (juice.particles.activeCount == 0) juice.frameBudget.reset()
        val cap = juice.frameBudget.activeParticleCap()
        juice.setConfettiWidth(width)

        for (cell in ui.pendingBurstCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            juice.particles.emitBurst(px, py, count = 18, color = GameColors.CoinGold, maxActive = cap)
        }
        for ((from, to) in ui.pendingSparkPairs) {
            val (fx, fy) = HexGeometry.hexToPixel(from, hexSize, centerX, centerY)
            val (tx, ty) = HexGeometry.hexToPixel(to, hexSize, centerX, centerY)
            juice.particles.emitSparks(fx, fy, tx, ty, count = 10, color = Color.White, maxActive = cap)
        }
        if (ui.pendingConfetti) {
            juice.particles.emitConfetti(
                width = width,
                count = 48,
                colors = listOf(
                    GameColors.CoinGold,
                    GameColors.Success,
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color.White
                ),
                maxActive = cap
            )
        }
        juice.consumePendingEmits()
        // Only redraw while an effect is alive; lifecycle pauses the frame clock offscreen.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = 0L
            while (juice.particles.activeCount > 0 || juice.uiState.value.hitStopRemainingMs > 0) {
                withFrameNanos { nanos ->
                    if (lastNanos != 0L) {
                        val elapsedMs = (nanos - lastNanos) / 1_000_000f
                        juice.frameBudget.recordFrameMs(elapsedMs)
                        val dtMs = elapsedMs.coerceIn(0f, 50f)
                        juice.particles.tick(dtMs / 1000f * juice.tickFeelClocks(dtMs))
                        frame++
                    }
                    lastNanos = nanos
                }
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged { boardSize = it }
    ) {
        @Suppress("UNUSED_EXPRESSION")
        frame
        for (p in juice.particles.activeParticles) {
            if (!p.active) continue
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x, p.y),
                style = Fill
            )
        }
    }
}
