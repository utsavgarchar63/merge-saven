package com.mergeseven.game.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.mergeseven.game.core.audio.GameFont
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.meta.CosmeticCatalog
import com.mergeseven.game.ui.theme.GameColors

/** Shared board, tray, ghost and cosmetic presentation. Never owns game geometry or state. */
enum class TileMaterial {
    JEWEL, MARBLE, NEON, GOLD;

    companion object {
        fun of(themeId: String): TileMaterial = when (themeId) {
            CosmeticCatalog.TILE_MARBLE -> MARBLE
            CosmeticCatalog.TILE_NEON -> NEON
            CosmeticCatalog.TILE_PREMIUM -> GOLD
            else -> JEWEL
        }
    }
}

@Composable
internal fun rememberTileTypeface(): Typeface {
    val context = LocalContext.current
    return remember(context) { GameFont.bold(context) }
}

private val unitHex = Path().apply {
    moveTo(1f, 0f); lineTo(0.5f, -0.8660254f); lineTo(-0.5f, -0.8660254f)
    lineTo(-1f, 0f); lineTo(-0.5f, 0.8660254f); lineTo(0.5f, 0.8660254f); close()
}
private val upperEdge = Path().apply {
    moveTo(-0.94f, -0.015f); lineTo(-0.47f, -0.814f); lineTo(0.47f, -0.814f)
}
private val lowerEdge = Path().apply {
    moveTo(-0.47f, 0.814f); lineTo(0.47f, 0.814f); lineTo(0.94f, 0.015f)
}
private val marbleVein = Path().apply {
    moveTo(-0.69f, -0.33f); cubicTo(-0.51f, -0.53f, -0.38f, -0.47f, -0.28f, -0.67f)
    moveTo(0.28f, 0.67f); cubicTo(0.4f, 0.49f, 0.57f, 0.57f, 0.67f, 0.35f)
}

private data class SurfaceKey(val color: Int, val material: TileMaterial)
private class TileSurface(color: Color, material: TileMaterial) {
    val side = lerp(color, Color.Black, if (material == TileMaterial.MARBLE) 0.32f else 0.42f)
    val rim = if (material == TileMaterial.GOLD) lerp(color, GameColors.CoinGold, 0.65f)
        else lerp(color, Color.White, 0.34f)
    // Preserve the actual theme fill across the central number area for contrast.
    val face = Brush.verticalGradient(
        0f to lerp(color, Color.White, if (material == TileMaterial.MARBLE) 0.10f else 0.22f),
        0.27f to color, 0.73f to color, 1f to lerp(color, Color.Black, 0.12f),
        startY = -0.866f, endY = 0.866f
    )
}
private val surfaceCache = LruCache<SurfaceKey, TileSurface>(64)
private data class TextKey(val value: Int, val color: Int, val textSize: Int, val radius: Int, val font: Typeface)
private val textCache = LruCache<TextKey, Paint>(128)

private fun usableGeometry(x: Float, y: Float, radius: Float) =
    x.isFinite() && y.isFinite() && radius.isFinite() && radius > 0f

/** Recessed cells use the same normalized hex as tiles; hit testing stays in HexGeometry. */
internal fun DrawScope.drawHexCell(centerX: Float, centerY: Float, size: Float, color: Color) {
    if (!usableGeometry(centerX, centerY, size)) return
    val radius = size * 0.95f
    withTransform({ translate(centerX, centerY); scale(radius, radius, Offset.Zero) }) {
        drawPath(unitHex, Color.Black.copy(alpha = 0.25f))
        drawPath(unitHex, color)
        drawPath(unitHex, Color.Black.copy(alpha = 0.22f), style = Stroke(0.045f))
        drawPath(upperEdge, Color.Black.copy(alpha = 0.35f), style = Stroke(0.04f))
        drawPath(lowerEdge, Color.White.copy(alpha = 0.16f), style = Stroke(0.025f))
    }
}

/** A translucent fill and explicit rim remain visible on occupied cells without hiding numbers. */
internal fun DrawScope.drawHexHighlight(centerX: Float, centerY: Float, size: Float, color: Color) {
    if (!usableGeometry(centerX, centerY, size)) return
    withTransform({ translate(centerX, centerY); scale(size * 0.94f, size * 0.94f, Offset.Zero) }) {
        drawPath(unitHex, color.copy(alpha = 0.12f))
        drawPath(unitHex, color.copy(alpha = 0.22f), style = Stroke(0.12f))
        drawPath(unitHex, color.copy(alpha = 0.95f), style = Stroke(0.045f))
    }
}

internal fun DrawScope.drawHexTile(
    centerX: Float, centerY: Float, size: Float, color: Color, value: Int,
    trait: TileTrait = TileTrait.NORMAL, freezeStage: Int = 0, multiplierFactor: Int = 1,
    showShapeCue: Boolean = false, fontScale: Float = 1f,
    material: TileMaterial = TileMaterial.JEWEL, typeface: Typeface = Typeface.DEFAULT_BOLD
) {
    if (!usableGeometry(centerX, centerY, size)) return
    val radius = size * 0.88f
    val key = SurfaceKey(color.toArgb(), material)
    val surface = surfaceCache.get(key) ?: TileSurface(color, material).also { surfaceCache.put(key, it) }
    val depth = if (material == TileMaterial.MARBLE) 0.055f else 0.09f
    withTransform({ translate(centerX, centerY); scale(radius, radius, Offset.Zero) }) {
        withTransform({ translate(0.035f, depth + 0.045f) }) {
            drawPath(unitHex, Color.Black.copy(alpha = 0.30f))
        }
        withTransform({ translate(0f, depth) }) { drawPath(unitHex, surface.side) }
        drawPath(unitHex, surface.rim)
        withTransform({ scale(0.91f, 0.91f, Offset.Zero) }) { drawPath(unitHex, surface.face) }
        drawPath(upperEdge, Color.White.copy(alpha = 0.65f), style = Stroke(0.023f))
        drawPath(lowerEdge, surface.side.copy(alpha = 0.80f), style = Stroke(0.025f))
        if (material == TileMaterial.NEON) {
            drawPath(unitHex, color.copy(alpha = 0.18f), style = Stroke(0.13f))
            drawPath(unitHex, lerp(color, Color.White, 0.65f), style = Stroke(0.022f))
        } else if (material == TileMaterial.MARBLE) {
            drawPath(marbleVein, Color.White.copy(alpha = 0.27f), style = Stroke(0.02f))
        }
    }
    drawTraitSilhouette(centerX, centerY, radius, trait, freezeStage, multiplierFactor)
    if (showShapeCue) drawValueShapeCue(centerX, centerY, radius, value)

    val scale = if (fontScale.isFinite()) fontScale.coerceIn(1f, 1.6f) else 1f
    val baseText = radius * when { value >= 1000 -> 0.40f; value >= 100 -> 0.50f; else -> 0.64f }
    val textSize = (baseText * scale).coerceAtMost(radius * 0.72f)
    val textKey = TextKey(value, GameColors.textOnTile(color).toArgb(), textSize.toBits(), radius.toBits(), typeface)
    val paint = textCache.get(textKey) ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = textKey.color; this.textSize = textSize; this.typeface = typeface
        textAlign = Paint.Align.CENTER
        val width = measureText(value.toString())
        if (width > radius * 1.34f) this.textSize *= radius * 1.34f / width
    }.also { textCache.put(textKey, it) }
    drawContext.canvas.nativeCanvas.drawText(value.toString(), centerX,
        centerY - (paint.ascent() + paint.descent()) / 2f, paint)
}
