package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * The state of the hex board.
 * See Master Plan Section 8.4.
 *
 * @param cells Map of hex coordinates to tiles. Null value means the cell is empty.
 * @param playableCells The set of coordinates that are valid play positions.
 */
@Serializable
data class BoardState(
    val cells: Map<HexCoord, Tile?>,
    val playableCells: Set<HexCoord>
) {
    /**
     * Returns all non-null tiles currently on the board.
     */
    fun activeTiles(): List<Tile> =
        cells.values.filterNotNull()

    /**
     * Returns all empty playable cells.
     */
    fun emptyCells(): Set<HexCoord> =
        playableCells.filter { cells[it] == null }.toSet()

    /**
     * Returns the tile at the given coordinate, or null if empty or out of bounds.
     */
    fun tileAt(coord: HexCoord): Tile? = cells[coord]

    /**
     * Returns true if the given coordinate is a valid playable cell.
     */
    fun isPlayable(coord: HexCoord): Boolean =
        coord in playableCells

    /**
     * Returns true if the given coordinate is empty and playable.
     */
    fun isEmpty(coord: HexCoord): Boolean =
        coord in playableCells && cells[coord] == null

    /**
     * Returns a new BoardState with the given tile placed.
     */
    fun withTile(tile: Tile): BoardState =
        copy(cells = cells + (tile.cell to tile))

    /**
     * Returns a new BoardState with the tile at the given coordinate removed.
     */
    fun withoutTile(coord: HexCoord): BoardState =
        copy(cells = cells + (coord to null))

    /**
     * Number of occupied cells.
     */
    val occupiedCount: Int get() = cells.values.count { it != null }

    /**
     * Number of total playable cells.
     */
    val totalPlayable: Int get() = playableCells.size

    /**
     * Board fill ratio (0.0 = empty, 1.0 = full).
     */
    val fillRatio: Float get() =
        if (totalPlayable == 0) 0f
        else occupiedCount.toFloat() / totalPlayable.toFloat()
}
