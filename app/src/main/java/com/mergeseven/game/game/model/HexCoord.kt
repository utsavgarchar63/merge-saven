package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * Axial hex coordinate using the (q, r) system.
 * See Master Plan Section 8.1.
 *
 * Axial coordinates are the standard representation for hex grids.
 * The implicit third coordinate s = -q - r (cube coordinates).
 */
@Serializable
data class HexCoord(
    val q: Int,
    val r: Int
) {
    /** Implicit cube coordinate s. */
    val s: Int get() = -q - r

    /** Add two hex coordinates. */
    operator fun plus(other: HexCoord): HexCoord =
        HexCoord(q + other.q, r + other.r)

    /** Subtract two hex coordinates. */
    operator fun minus(other: HexCoord): HexCoord =
        HexCoord(q - other.q, r - other.r)

    /** Scale the coordinate by a factor. */
    operator fun times(scale: Int): HexCoord =
        HexCoord(q * scale, r * scale)

    /**
     * Returns all six neighbors of this hex cell.
     */
    fun neighbors(): List<HexCoord> =
        HEX_DIRECTIONS.map { this + it }

    /**
     * Returns the neighbor in the given direction index (0-5).
     */
    fun neighbor(direction: Int): HexCoord =
        this + HEX_DIRECTIONS[direction % HEX_DIRECTIONS.size]

    /**
     * Hex distance from this cell to another (Manhattan distance on hex grid).
     */
    fun distanceTo(other: HexCoord): Int {
        val dq = (q - other.q)
        val dr = (r - other.r)
        val ds = (s - other.s)
        return maxOf(
            kotlin.math.abs(dq),
            kotlin.math.abs(dr),
            kotlin.math.abs(ds)
        )
    }

    /**
     * Rotate this coordinate 60° clockwise around the origin.
     * In cube coordinates: (q, r, s) -> (-r, -s, -q)
     * Converted back to axial: q' = -r, r' = -s = -(−q−r) = q + r
     */
    fun rotateClockwise60(): HexCoord =
        HexCoord(-r, q + r)

    /**
     * Rotate this coordinate 60° counter-clockwise around the origin.
     * In cube coordinates: (q, r, s) -> (-s, -q, -r)
     * Converted back to axial: q' = -s = q + r, r' = -q
     */
    fun rotateCounterClockwise60(): HexCoord =
        HexCoord(q + r, -q)

    /**
     * Rotate by N steps of 60° clockwise.
     */
    fun rotate(steps: Int): HexCoord {
        val normalizedSteps = ((steps % 6) + 6) % 6
        var result = this
        repeat(normalizedSteps) {
            result = result.rotateClockwise60()
        }
        return result
    }

    companion object {
        /** Origin hex cell. */
        val ORIGIN = HexCoord(0, 0)
    }
}

/**
 * The six axial direction vectors for flat-top hex neighbors.
 * Ordered clockwise starting from the right.
 */
val HEX_DIRECTIONS = listOf(
    HexCoord(1, 0),    // East
    HexCoord(1, -1),   // Northeast
    HexCoord(0, -1),   // Northwest
    HexCoord(-1, 0),   // West
    HexCoord(-1, 1),   // Southwest
    HexCoord(0, 1)     // Southeast
)
