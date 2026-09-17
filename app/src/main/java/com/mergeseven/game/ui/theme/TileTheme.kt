package com.mergeseven.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.meta.CosmeticCatalog

/**
 * Tile colour tokens for AF5-03. Must not affect gameplay / hit testing.
 */
interface TileTheme {
    val id: String
    fun tileColor(value: Int): Color
    fun tileTextColor(value: Int): Color = Color.White
    val strokeAlpha: Float get() = 0.70f
    val highlightAlpha: Float get() = 0.15f
}

object ClassicTileTheme : TileTheme {
    override val id: String = CosmeticCatalog.TILE_CLASSIC
    override fun tileColor(value: Int): Color = GameColors.tileColor(value)
    override fun tileTextColor(value: Int): Color = GameColors.tileTextColor(value)
}

object MarbleTileTheme : TileTheme {
    override val id: String = CosmeticCatalog.TILE_MARBLE
    override fun tileColor(value: Int): Color =
        desaturate(GameColors.tileColor(value), 0.35f)
}

object NeonTileTheme : TileTheme {
    override val id: String = CosmeticCatalog.TILE_NEON
    override fun tileColor(value: Int): Color =
        saturate(GameColors.tileColor(value), 0.45f)
    override val highlightAlpha: Float = 0.28f
}

object SeasonalTileTheme : TileTheme {
    override val id: String = CosmeticCatalog.TILE_SEASONAL
    override fun tileColor(value: Int): Color =
        warmShift(GameColors.tileColor(value))
}

object TileThemes {
    fun of(id: String): TileTheme = when (id) {
        CosmeticCatalog.TILE_MARBLE -> MarbleTileTheme
        CosmeticCatalog.TILE_NEON -> NeonTileTheme
        CosmeticCatalog.TILE_SEASONAL -> SeasonalTileTheme
        else -> ClassicTileTheme
    }
}

private fun desaturate(color: Color, amount: Float): Color {
    val gray = (color.red + color.green + color.blue) / 3f
    return Color(
        red = color.red + (gray - color.red) * amount,
        green = color.green + (gray - color.green) * amount,
        blue = color.blue + (gray - color.blue) * amount,
        alpha = color.alpha
    )
}

private fun saturate(color: Color, amount: Float): Color {
    val gray = (color.red + color.green + color.blue) / 3f
    fun channel(c: Float) = (c + (c - gray) * amount).coerceIn(0f, 1f)
    return Color(channel(color.red), channel(color.green), channel(color.blue), color.alpha)
}

private fun warmShift(color: Color): Color = Color(
    red = (color.red * 1.08f).coerceAtMost(1f),
    green = (color.green * 0.95f).coerceIn(0f, 1f),
    blue = (color.blue * 0.85f).coerceIn(0f, 1f),
    alpha = color.alpha
)
