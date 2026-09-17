package com.mergeseven.game.game.model

import kotlin.math.abs
import kotlin.math.max
import kotlinx.serialization.Serializable

/**
 * The state of the hex board.
 * See Master Plan Section 8.4 / AF1-10–12.
 *
 * @param cells Map of hex coordinates to tiles. Null value means the cell is empty.
 * @param playableCells The set of coordinates that are valid play positions.
 * @param cellModifiers Optional per-cell modifiers (score pad, spawn vent, locked).
 */
@Serializable
data class BoardState(
    val cells: Map<HexCoord, Tile?>,
    val playableCells: Set<HexCoord>,
    val cellModifiers: Map<HexCoord, CellModifier> = emptyMap()
) {
    fun activeTiles(): List<Tile> =
        cells.values.filterNotNull()

    fun emptyCells(): Set<HexCoord> =
        playableCells.filter { cells[it] == null }.toSet()

    fun tileAt(coord: HexCoord): Tile? = cells[coord]

    fun isPlayable(coord: HexCoord): Boolean =
        coord in playableCells

    fun isEmpty(coord: HexCoord): Boolean =
        coord in playableCells && cells[coord] == null

    fun modifierAt(coord: HexCoord): CellModifier? = cellModifiers[coord]

    fun isLocked(coord: HexCoord): Boolean =
        cellModifiers[coord]?.type == CellModifierType.LOCKED

    fun withTile(tile: Tile): BoardState =
        copy(cells = cells + (tile.cell to tile))

    fun withoutTile(coord: HexCoord): BoardState =
        copy(cells = cells + (coord to null))

    fun withModifiers(modifiers: Map<HexCoord, CellModifier>): BoardState =
        copy(cellModifiers = modifiers)

    fun withModifierCleared(coord: HexCoord): BoardState =
        copy(cellModifiers = cellModifiers - coord)

    val occupiedCount: Int get() = cells.values.count { it != null }

    val totalPlayable: Int get() = playableCells.size

    val fillRatio: Float get() =
        if (totalPlayable == 0) 0f
        else occupiedCount.toFloat() / totalPlayable.toFloat()

    /** Bounding hex radius for Canvas fit (AF1-10 non-radial boards). */
    fun displayRadius(): Int =
        playableCells.maxOfOrNull { max(abs(it.q), max(abs(it.r), abs(it.s))) }
            ?: 0
}
