package com.mergeseven.game.core.crash

import com.mergeseven.game.game.model.BoardState

/**
 * Stable, PII-free fingerprint of board occupancy for Crashlytics (AF6-03).
 */
object BoardHash {
    fun of(board: BoardState): String {
        val tiles = board.activeTiles()
            .sortedWith(compareBy({ it.cell.q }, { it.cell.r }, { it.value }))
            .joinToString(";") { "${it.cell.q},${it.cell.r}:${it.value}" }
        return tiles.hashCode().toUInt().toString(16)
    }
}
