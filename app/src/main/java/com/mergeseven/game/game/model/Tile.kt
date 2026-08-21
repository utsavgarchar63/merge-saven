package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * A numbered tile placed on the board.
 * See Master Plan Section 8.2.
 *
 * @param id Unique identifier for this tile instance.
 * @param value The numeric value (2, 4, 8, 16, ...).
 * @param cell The board position this tile occupies.
 */
@Serializable
data class Tile(
    val id: Long,
    val value: Int,
    val cell: HexCoord
)
