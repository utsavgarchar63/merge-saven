package com.mergeseven.game.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.mergeseven.game.game.model.TileTrait
import kotlin.math.cos
import kotlin.math.sin

/**
 * Colourblind-safe silhouette overlays for special tiles (AF1-09 / AF11).
 * Geometry follows assets/art/tile_trait_silhouettes.png reference.
 */
fun DrawScope.drawTraitSilhouette(
    centerX: Float,
    centerY: Float,
    size: Float,
    trait: TileTrait,
    freezeStage: Int = 0,
    multiplierFactor: Int = 1
) {
    when (trait) {
        TileTrait.NORMAL -> Unit
        TileTrait.BOMB -> drawBombSilhouette(centerX, centerY, size)
        TileTrait.WILDCARD -> drawWildcardSilhouette(centerX, centerY, size)
        TileTrait.FROZEN -> drawFrozenSilhouette(centerX, centerY, size, freezeStage)
        TileTrait.STONE -> drawStoneSilhouette(centerX, centerY, size)
        TileTrait.MULTIPLIER -> drawMultiplierSilhouette(centerX, centerY, size, multiplierFactor)
    }
}

private fun DrawScope.drawBombSilhouette(cx: Float, cy: Float, size: Float) {
    val r = size * 0.22f
    drawCircle(Color.White.copy(alpha = 0.9f), radius = r, center = Offset(cx, cy - size * 0.08f))
    val fuse = Path().apply {
        moveTo(cx + r * 0.4f, cy - size * 0.08f - r * 0.7f)
        quadraticTo(
            cx + size * 0.28f,
            cy - size * 0.42f,
            cx + size * 0.18f,
            cy - size * 0.48f
        )
    }
    drawPath(fuse, Color.White, style = Stroke(width = size * 0.06f))
    for (i in 0 until 6) {
        val angle = Math.toRadians((60.0 * i) - 90.0)
        val inner = size * 0.72f
        val outer = size * 0.92f
        val ix = cx + inner * cos(angle).toFloat()
        val iy = cy + inner * sin(angle).toFloat()
        val ox = cx + outer * cos(angle).toFloat()
        val oy = cy + outer * sin(angle).toFloat()
        drawLine(Color.White.copy(alpha = 0.85f), Offset(ix, iy), Offset(ox, oy), strokeWidth = size * 0.05f)
    }
}

private fun DrawScope.drawWildcardSilhouette(cx: Float, cy: Float, size: Float) {
    val arc = Path().apply {
        val r = size * 0.55f
        addArc(
            oval = Rect(cx - r, cy - r, cx + r, cy + r),
            startAngleDegrees = 200f,
            sweepAngleDegrees = 140f
        )
    }
    drawPath(arc, Color.White.copy(alpha = 0.9f), style = Stroke(width = size * 0.08f))
    drawCircle(
        color = Color.White,
        radius = size * 0.08f,
        center = Offset(cx + size * 0.35f, cy + size * 0.32f)
    )
}

private fun DrawScope.drawFrozenSilhouette(cx: Float, cy: Float, size: Float, freezeStage: Int) {
    val stroke = size * 0.05f
    val white = Color.White.copy(alpha = 0.85f)
    for (deg in listOf(0.0, 60.0, 120.0)) {
        val a = Math.toRadians(deg)
        val dx = (size * 0.55f * cos(a)).toFloat()
        val dy = (size * 0.55f * sin(a)).toFloat()
        drawLine(white, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), strokeWidth = stroke)
    }
    drawLine(
        white.copy(alpha = 0.6f),
        Offset(cx - size * 0.2f, cy - size * 0.35f),
        Offset(cx - size * 0.05f, cy - size * 0.1f),
        strokeWidth = stroke * 0.7f
    )
    // Cracked ice (stage 1): extra fractures
    if (freezeStage == 1) {
        drawLine(
            white.copy(alpha = 0.95f),
            Offset(cx - size * 0.35f, cy + size * 0.05f),
            Offset(cx + size * 0.1f, cy - size * 0.25f),
            strokeWidth = stroke * 0.9f
        )
        drawLine(
            white.copy(alpha = 0.9f),
            Offset(cx + size * 0.05f, cy + size * 0.3f),
            Offset(cx + size * 0.35f, cy - size * 0.05f),
            strokeWidth = stroke * 0.8f
        )
    }
}

private fun DrawScope.drawStoneSilhouette(cx: Float, cy: Float, size: Float) {
    val rocky = Path().apply {
        moveTo(cx - size * 0.45f, cy + size * 0.1f)
        lineTo(cx - size * 0.25f, cy - size * 0.35f)
        lineTo(cx + size * 0.05f, cy - size * 0.2f)
        lineTo(cx + size * 0.4f, cy - size * 0.3f)
        lineTo(cx + size * 0.48f, cy + size * 0.15f)
        lineTo(cx + size * 0.15f, cy + size * 0.4f)
        lineTo(cx - size * 0.35f, cy + size * 0.35f)
        close()
    }
    drawPath(rocky, Color.White.copy(alpha = 0.25f))
    drawPath(rocky, Color.White.copy(alpha = 0.7f), style = Stroke(width = size * 0.05f))
    drawLine(
        Color.White.copy(alpha = 0.75f),
        Offset(cx - size * 0.1f, cy - size * 0.15f),
        Offset(cx + size * 0.2f, cy + size * 0.2f),
        strokeWidth = size * 0.04f
    )
}

private fun DrawScope.drawMultiplierSilhouette(
    cx: Float,
    cy: Float,
    size: Float,
    multiplierFactor: Int
) {
    val badgeCx = cx + size * 0.38f
    val badgeCy = cy - size * 0.38f
    val r = size * 0.22f
    drawCircle(Color(0xFFFFD54A), radius = r, center = Offset(badgeCx, badgeCy))
    drawCircle(Color(0xFF6F3B24), radius = r, center = Offset(badgeCx, badgeCy), style = Stroke(width = size * 0.04f))

    // ×N cue drawn as two short strokes forming an X plus a tiny tick count is noisy;
    // prefer Android text for a crisp "×2" / "×3" badge label.
    val label = "×${multiplierFactor.coerceIn(2, 3)}"
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6F3B24")
        textSize = size * 0.22f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        badgeCx,
        badgeCy + paint.textSize * 0.35f,
        paint
    )
}
