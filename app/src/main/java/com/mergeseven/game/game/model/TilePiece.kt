package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * A single cell within a piece, defined as an offset from the piece's anchor.
 * See Master Plan Section 8.3.
 *
 * @param offset Relative position from the piece's origin.
 * @param value The numeric value for this cell.
 * @param trait Forwarded onto the board tile when placed (AF1).
 * @param multiplierFactor Forwarded for MULTIPLIER cells.
 * @param freezeStage Forwarded for FROZEN cells.
 */
@Serializable
data class PieceCell(
    val offset: HexCoord,
    val value: Int,
    val trait: TileTrait = TileTrait.NORMAL,
    val multiplierFactor: Int = 1,
    val freezeStage: Int = 0
)

/**
 * Absolute board position of a piece cell after placing at an origin.
 */
data class AbsolutePieceCell(
    val coord: HexCoord,
    val value: Int,
    val trait: TileTrait = TileTrait.NORMAL,
    val multiplierFactor: Int = 1,
    val freezeStage: Int = 0
)

/**
 * A multi-tile piece that the player places on the board.
 * See Master Plan Section 8.3, Section 13.
 *
 * @param id Unique identifier for this piece.
 * @param cells The cells that make up this piece, each with an offset and value.
 * @param rotation Current rotation step (0-5, each = 60°).
 */
@Serializable
data class TilePiece(
    val id: Long,
    val cells: List<PieceCell>,
    val rotation: Int = 0
) {
    /**
     * Returns the piece cells with rotation applied.
     */
    fun rotatedCells(): List<PieceCell> {
        if (rotation == 0) return cells
        return cells.map { cell ->
            cell.copy(offset = cell.offset.rotate(rotation))
        }
    }

    /**
     * Returns a copy of this piece rotated clockwise by one step (60°).
     */
    fun rotateClockwise(): TilePiece =
        copy(rotation = (rotation + 1) % 6)

    /**
     * Returns a copy rotated counter-clockwise by one step (60°).
     */
    fun rotateCounterClockwise(): TilePiece =
        copy(rotation = (rotation + 5) % 6) // +5 ≡ -1 mod 6

    /**
     * Absolute board positions (coord + value only) for hover / canPlace.
     */
    fun absoluteCells(origin: HexCoord): List<Pair<HexCoord, Int>> =
        absolutePieceCells(origin).map { it.coord to it.value }

    /**
     * Absolute board cells including trait payload for placement.
     */
    fun absolutePieceCells(origin: HexCoord): List<AbsolutePieceCell> =
        rotatedCells().map { cell ->
            AbsolutePieceCell(
                coord = origin + cell.offset,
                value = cell.value,
                trait = cell.trait,
                multiplierFactor = cell.multiplierFactor,
                freezeStage = cell.freezeStage
            )
        }
}

/**
 * Predefined piece shape templates.
 * See Master Plan Section 92.
 */
data class PieceShape(
    val id: String,
    val cells: List<PieceCell>
)
