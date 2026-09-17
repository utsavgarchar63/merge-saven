package com.mergeseven.game.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Game color palette.
 * See Master Plan Section 35.
 *
 * These are starting values for development and prototyping.
 */
object GameColors {

    // ──────────────────────────────────────────────
    // Wood / Background (Harmonized to App Icon)
    // ──────────────────────────────────────────────
    val WoodDark = Color(0xFF4A2518)
    val WoodMid = Color(0xFF7A3E26)
    val WoodLight = Color(0xFFA85D3B)

    // ──────────────────────────────────────────────
    // Tile Colors (mapped to tile values)
    // ──────────────────────────────────────────────
    val TileBlue = Color(0xFF2CA5E0)
    val TileGreen = Color(0xFF48D368)
    val TileRed = Color(0xFFF0534C)
    val TilePurple = Color(0xFF8B5CF6)
    val TilePink = Color(0xFFEC4899)
    val TileGold = Color(0xFFFBBF24)
    val TileTeal = Color(0xFF14B8A6)
    val TileOrange = Color(0xFFF97316)
    val TileIndigo = Color(0xFF6366F1)
    val TileLime = Color(0xFF84CC16)
    val TileCyan = Color(0xFF06B6D4)

    // ──────────────────────────────────────────────
    // Text
    // ──────────────────────────────────────────────
    val TextWhite = Color(0xFFFFFFFF)
    val TextDark = Color(0xFF28130B)

    // ──────────────────────────────────────────────
    // UI Accents
    // ──────────────────────────────────────────────
    val CoinGold = Color(0xFFFFD54A)
    val Success = Color(0xFF48D368)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)

    // ──────────────────────────────────────────────
    // Board
    // ──────────────────────────────────────────────
    val BoardCellEmpty = Color(0x33FFFFFF)
    val BoardCellHighlight = Color(0x66FFD54A)
    val BoardCellHint = Color(0x8838BDF8)
    val BoardCellInvalid = Color(0x44EF4444)

    // ──────────────────────────────────────────────
    // Tile Value → Color Mapping
    // ──────────────────────────────────────────────

    /**
     * Returns the fill color for a tile of the given value.
     * Data-driven mapping — not hard-coded to a maximum value.
     */
    fun tileColor(value: Int): Color {
        return when (value) {
            2 -> TileBlue
            4 -> TileGreen
            8 -> TileRed
            16 -> TilePurple
            32 -> TilePink
            64 -> TileGold
            128 -> TileTeal
            256 -> TileOrange
            512 -> TileIndigo
            1024 -> TileLime
            2048 -> TileCyan
            else -> {
                // For values beyond 2048, cycle through colors
                val colors = listOf(
                    TileBlue, TileGreen, TileRed, TilePurple,
                    TilePink, TileGold, TileTeal, TileOrange
                )
                val index = (Integer.numberOfTrailingZeros(value) - 1) % colors.size
                colors[index.coerceAtLeast(0)]
            }
        }
    }

    /**
     * Returns the text color for a tile (ensuring contrast).
     */
    fun tileTextColor(value: Int): Color {
        return TextWhite
    }
}
