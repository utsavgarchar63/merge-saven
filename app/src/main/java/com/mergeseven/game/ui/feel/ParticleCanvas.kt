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
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.model.HexCoord
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

    LaunchedEffect(juice) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dtMs = ((nanos - lastNanos) / 1_000_000f).coerceIn(0f, 50f)
                    juice.frameBudget.recordFrameMs(dtMs)
                    val scale = juice.tickFeelClocks(dtMs)
                    juice.particles.tick((dtMs / 1000f) * scale)
                    frame++
                }
                lastNanos = nanos
            }
        }
    }

    LaunchedEffect(ui.revision, boardSize) {
        if (ui.reduceMotion) {
            juice.consumePendingEmits()
            return@LaunchedEffect
        }
        if (boardSize.width == 0) {
            return@LaunchedEffect
        }
        if (ui.pendingBurstCells.isEmpty() &&
            ui.pendingSparkPairs.isEmpty() &&
            !ui.pendingConfetti
        ) {
            return@LaunchedEffect
        }

        val width = boardSize.width.toFloat()
        val height = boardSize.height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val hexSize = HexGeometry.calculateHexSize(boardRadius, width, height, 8f)
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
