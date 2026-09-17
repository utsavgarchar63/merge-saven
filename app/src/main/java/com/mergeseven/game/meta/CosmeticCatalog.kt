package com.mergeseven.game.meta

enum class CosmeticKind {
    TILE,
    BOARD
}

data class CosmeticDef(
    val id: String,
    val title: String,
    val kind: CosmeticKind,
    val coinCost: Int,
    /** Player level required to unlock for free (0 = none). */
    val unlockAtLevel: Int = 0,
    val startingOwned: Boolean = false
)

object CosmeticCatalog {
    const val TILE_CLASSIC = "classic"
    const val TILE_MARBLE = "tile_marble"
    const val TILE_NEON = "tile_neon"
    const val TILE_SEASONAL = "tile_seasonal"
    /** AF9-08 Premium exclusive. */
    const val TILE_PREMIUM = "tile_premium"
    const val BOARD_WOOD = "wood"
    const val BOARD_MARBLE = "board_marble"
    const val BOARD_NEON = "board_neon"
    const val BOARD_SEASONAL = "board_seasonal"

    val all: List<CosmeticDef> = listOf(
        CosmeticDef(TILE_CLASSIC, "Classic Tiles", CosmeticKind.TILE, coinCost = 0, startingOwned = true),
        CosmeticDef(TILE_MARBLE, "Marble Tiles", CosmeticKind.TILE, coinCost = 150, unlockAtLevel = 3),
        CosmeticDef(TILE_NEON, "Neon Tiles", CosmeticKind.TILE, coinCost = 200, unlockAtLevel = 5),
        CosmeticDef(TILE_SEASONAL, "Seasonal Tiles", CosmeticKind.TILE, coinCost = 180, unlockAtLevel = 0),
        CosmeticDef(TILE_PREMIUM, "Premium Tiles", CosmeticKind.TILE, coinCost = 0, unlockAtLevel = 0),
        CosmeticDef(BOARD_WOOD, "Wood Board", CosmeticKind.BOARD, coinCost = 0, startingOwned = true),
        CosmeticDef(BOARD_MARBLE, "Marble Board", CosmeticKind.BOARD, coinCost = 200, unlockAtLevel = 8),
        CosmeticDef(BOARD_NEON, "Neon Board", CosmeticKind.BOARD, coinCost = 250, unlockAtLevel = 10),
        CosmeticDef(BOARD_SEASONAL, "Seasonal Board", CosmeticKind.BOARD, coinCost = 220)
    )

    fun byId(id: String): CosmeticDef? = all.firstOrNull { it.id == id }

    fun unlockId(id: String): String = "cosmetic_$id"

    fun tiles(): List<CosmeticDef> = all.filter { it.kind == CosmeticKind.TILE }
    fun boards(): List<CosmeticDef> = all.filter { it.kind == CosmeticKind.BOARD }
}
