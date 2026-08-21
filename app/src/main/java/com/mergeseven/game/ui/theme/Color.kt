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
    // Wood / Background
    // ──────────────────────────────────────────────
    val WoodDark = Color(0xFF6F3B24)
    val WoodMid = Color(0xFF9B5A35)
    val WoodLight = Color(0xFFC98953)

    // ──────────────────────────────────────────────
    // Tile Colors (mapped to tile values)
    // ──────────────────────────────────────────────
    val TileBlue = Color(0xFF35A9E0)
    val TileGreen = Color(0xFF62D95C)
    val TileRed = Color(0xFFEB665B)
    val TilePurple = Color(0xFF7567DD)
    val TilePink = Color(0xFFD955A8)
    val TileGold = Color(0xFFF1B62B)
    val TileTeal = Color(0xFF26C6DA)
    val TileOrange = Color(0xFFFF8A65)
    val TileIndigo = Color(0xFF5C6BC0)
    val TileLime = Color(0xFFAED581)
    val TileCyan = Color(0xFF4DD0E1)

    // ──────────────────────────────────────────────
    // Text
    // ──────────────────────────────────────────────
    val TextWhite = Color(0xFFFFFFFF)
    val TextDark = Color(0xFF3C241A)

    // ──────────────────────────────────────────────
    // UI Accents
    // ──────────────────────────────────────────────
    val CoinGold = Color(0xFFFFD54A)
    val Success = Color(0xFF58D66F)
    val Warning = Color(0xFFFFB648)
    val Error = Color(0xFFE9534F)

    // ──────────────────────────────────────────────
    // Board
    // ──────────────────────────────────────────────
    val BoardCellEmpty = Color(0x33FFFFFF)
    val BoardCellHighlight = Color(0x66FFD54A)
    val BoardCellInvalid = Color(0x44E9534F)

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
