package com.mergeseven.game.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mergeseven.game.ui.theme.ColourblindPalette

/**
 * AF11-01 value→shape silhouettes drawn under tile numerals when colourblind mode is on.
 */
fun DrawScope.drawValueShapeCue(
    centerX: Float,
    centerY: Float,
    hexSize: Float,
    value: Int
) {
    val index = ColourblindPalette.valueIndex(value) % 8
    val s = hexSize * 0.28f
    val color = Color.White.copy(alpha = 0.55f)
    val cy = centerY + hexSize * 0.28f
    when (index) {
        0 -> drawCircle(color, radius = s * 0.55f, center = Offset(centerX, cy))
        1 -> drawRect(
            color,
            topLeft = Offset(centerX - s * 0.5f, cy - s * 0.5f),
            size = Size(s, s)
        )
        2 -> {
            val path = Path().apply {
                moveTo(centerX, cy - s * 0.6f)
                lineTo(centerX + s * 0.55f, cy + s * 0.45f)
                lineTo(centerX - s * 0.55f, cy + s * 0.45f)
                close()
            }
            drawPath(path, color)
        }
        3 -> {
            val path = Path().apply {
                moveTo(centerX, cy - s * 0.55f)
                lineTo(centerX + s * 0.55f, cy)
                lineTo(centerX, cy + s * 0.55f)
                lineTo(centerX - s * 0.55f, cy)
                close()
            }
            drawPath(path, color)
        }
        4 -> {
            drawLine(color, Offset(centerX - s * 0.5f, cy), Offset(centerX + s * 0.5f, cy), strokeWidth = s * 0.25f)
            drawLine(color, Offset(centerX, cy - s * 0.5f), Offset(centerX, cy + s * 0.5f), strokeWidth = s * 0.25f)
        }
        5 -> {
            val path = Path().apply {
                moveTo(centerX - s * 0.5f, cy + s * 0.35f)
                lineTo(centerX, cy - s * 0.45f)
                lineTo(centerX + s * 0.5f, cy + s * 0.35f)
            }
            drawPath(path, color, style = Stroke(width = s * 0.22f))
        }
        6 -> {
            // Hexagon outline
            val path = Path()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i) - 30.0)
                val x = centerX + s * 0.55f * kotlin.math.cos(angle).toFloat()
                val y = cy + s * 0.55f * kotlin.math.sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color, style = Stroke(width = s * 0.18f))
        }
        else -> {
            // Star-ish: three short rays
            for (i in 0 until 3) {
                val angle = Math.toRadians(i * 60.0)
                val dx = s * 0.55f * kotlin.math.cos(angle).toFloat()
                val dy = s * 0.55f * kotlin.math.sin(angle).toFloat()
                drawLine(
                    color,
                    Offset(centerX - dx, cy - dy),
                    Offset(centerX + dx, cy + dy),
                    strokeWidth = s * 0.18f
                )
            }
        }
    }
}
