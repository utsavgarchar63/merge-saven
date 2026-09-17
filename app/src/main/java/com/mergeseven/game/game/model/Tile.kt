package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * A numbered tile placed on the board.
 * See Master Plan Section 8.2 / AF1 special tiles.
 *
 * @param id Unique identifier for this tile instance.
 * @param value The numeric value (2, 4, 8, 16, ...); decorative for STONE.
 * @param cell The board position this tile occupies.
 * @param trait Special behaviour; silhouettes drawn in Canvas (AF1-09).
 * @param freezeStage FROZEN only: 2 = solid ice, 1 = cracked; else 0.
 * @param multiplierFactor MULTIPLIER only: 2 or 3; else 1.
 */
@Serializable
data class Tile(
    val id: Long,
    val value: Int,
    val cell: HexCoord,
    val trait: TileTrait = TileTrait.NORMAL,
    val freezeStage: Int = 0,
    val multiplierFactor: Int = 1
) {
    fun isMergeBlocking(): Boolean =
        trait == TileTrait.STONE || (trait == TileTrait.FROZEN && freezeStage > 0)

    companion object {
        fun normal(id: Long, value: Int, cell: HexCoord) =
            Tile(id = id, value = value, cell = cell, trait = TileTrait.NORMAL)

        fun bomb(id: Long, value: Int, cell: HexCoord) =
            Tile(id = id, value = value, cell = cell, trait = TileTrait.BOMB)

        fun wildcard(id: Long, value: Int, cell: HexCoord) =
            Tile(id = id, value = value, cell = cell, trait = TileTrait.WILDCARD)

        fun frozen(id: Long, value: Int, cell: HexCoord, stage: Int = 2) =
            Tile(
                id = id,
                value = value,
                cell = cell,
                trait = TileTrait.FROZEN,
                freezeStage = stage.coerceIn(1, 2)
            )

        fun stone(id: Long, cell: HexCoord, value: Int = 0) =
            Tile(id = id, value = value, cell = cell, trait = TileTrait.STONE)

        fun multiplier(id: Long, value: Int, cell: HexCoord, factor: Int) =
            Tile(
                id = id,
                value = value,
                cell = cell,
                trait = TileTrait.MULTIPLIER,
                multiplierFactor = factor.coerceIn(2, 3)
            )
    }
}
