package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * Represents a single cell on the hex board.
 * A cell is a position that can hold a tile or be empty.
 */
@Serializable
data class HexCell(
    val coord: HexCoord,
    val isPlayable: Boolean = true,
    val isBlocked: Boolean = false
)
