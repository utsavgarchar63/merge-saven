package com.mergeseven.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.meta.CosmeticCatalog

/**
 * Wraps an AF5 [TileTheme] so cosmetic transforms apply on a colourblind-safe base.
 */
class ColourblindTileTheme(
    private val delegate: TileTheme,
    private val mode: ColourblindMode
) : TileTheme {
    override val id: String = delegate.id
    override val strokeAlpha: Float get() = delegate.strokeAlpha
    override val highlightAlpha: Float get() = delegate.highlightAlpha

    override fun tileColor(value: Int): Color {
        if (mode == ColourblindMode.OFF) return delegate.tileColor(value)
        val base = ColourblindPalette.tileColor(mode, value)
        return when (delegate.id) {
            CosmeticCatalog.TILE_MARBLE -> desaturateCb(base, 0.35f)
            CosmeticCatalog.TILE_NEON -> saturateCb(base, 0.45f)
            CosmeticCatalog.TILE_SEASONAL -> warmShiftCb(base)
            else -> base
        }
    }

    override fun tileTextColor(value: Int): Color = delegate.tileTextColor(value)
}

fun TileTheme.withColourblindMode(mode: ColourblindMode): TileTheme =
    if (mode == ColourblindMode.OFF) this else ColourblindTileTheme(this, mode)

private fun desaturateCb(color: Color, amount: Float): Color {
    val gray = (color.red + color.green + color.blue) / 3f
    return Color(
        red = color.red + (gray - color.red) * amount,
        green = color.green + (gray - color.green) * amount,
        blue = color.blue + (gray - color.blue) * amount,
        alpha = color.alpha
    )
}

private fun saturateCb(color: Color, amount: Float): Color {
    val gray = (color.red + color.green + color.blue) / 3f
    fun channel(c: Float) = (c + (c - gray) * amount).coerceIn(0f, 1f)
    return Color(channel(color.red), channel(color.green), channel(color.blue), color.alpha)
}

private fun warmShiftCb(color: Color): Color = Color(
    red = (color.red * 1.08f).coerceAtMost(1f),
    green = (color.green * 0.95f).coerceIn(0f, 1f),
    blue = (color.blue * 0.85f).coerceIn(0f, 1f),
    alpha = color.alpha
)
