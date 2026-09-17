package com.mergeseven.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.meta.CosmeticCatalog
import com.mergeseven.game.ui.components.GameIcons

/**
 * Board / background tokens for AF5-04.
 */
data class BoardTheme(
    val id: String,
    val backgroundRes: Int,
    val overlayColor: Color,
    val cellEmpty: Color,
    val cellHighlight: Color,
    val cellHint: Color,
    val cellInvalid: Color
)

object BoardThemes {
    val Wood = BoardTheme(
        id = CosmeticCatalog.BOARD_WOOD,
        backgroundRes = GameIcons.WoodBackground,
        overlayColor = GameColors.WoodDark.copy(alpha = 0.35f),
        cellEmpty = GameColors.BoardCellEmpty,
        cellHighlight = GameColors.BoardCellHighlight,
        cellHint = GameColors.BoardCellHint,
        cellInvalid = GameColors.BoardCellInvalid
    )

    val Marble = BoardTheme(
        id = CosmeticCatalog.BOARD_MARBLE,
        backgroundRes = GameIcons.WoodBackground,
        overlayColor = Color(0xFF4A5568).copy(alpha = 0.55f),
        cellEmpty = Color(0x44E2E8F0),
        cellHighlight = Color(0x88CBD5E1),
        cellHint = Color(0x887DD3FC),
        cellInvalid = GameColors.BoardCellInvalid
    )

    val Neon = BoardTheme(
        id = CosmeticCatalog.BOARD_NEON,
        backgroundRes = GameIcons.WoodBackground,
        overlayColor = Color(0xFF0F172A).copy(alpha = 0.65f),
        cellEmpty = Color(0x3322D3EE),
        cellHighlight = Color(0x88A855F7),
        cellHint = Color(0x8822D3EE),
        cellInvalid = GameColors.BoardCellInvalid
    )

    val Seasonal = BoardTheme(
        id = CosmeticCatalog.BOARD_SEASONAL,
        backgroundRes = GameIcons.WoodBackground,
        overlayColor = Color(0xFF7C2D12).copy(alpha = 0.45f),
        cellEmpty = Color(0x33FDBA74),
        cellHighlight = Color(0x88FBBF24),
        cellHint = Color(0x88FDE68A),
        cellInvalid = GameColors.BoardCellInvalid
    )

    fun of(id: String): BoardTheme = when (id) {
        CosmeticCatalog.BOARD_MARBLE -> Marble
        CosmeticCatalog.BOARD_NEON -> Neon
        CosmeticCatalog.BOARD_SEASONAL -> Seasonal
        else -> Wood
    }
}
