package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.HexCoord
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Hex geometry utility for converting between axial coordinates and screen pixels.
 * See Master Plan Section 9.2.
 *
 * Uses flat-top hex orientation.
 * All calculations are based on the hex size (distance from center to corner).
 */
object HexGeometry {

    /**
     * Convert axial hex coordinate to pixel position (center of hex).
     * Flat-top hex layout.
     *
     * @param coord The axial hex coordinate.
     * @param size The hex size (center to corner distance in pixels).
     * @param centerX The pixel X of the board center (hex origin).
     * @param centerY The pixel Y of the board center (hex origin).
     * @return Pair of (pixelX, pixelY).
     */
    fun hexToPixel(
        coord: HexCoord,
        size: Float,
        centerX: Float = 0f,
        centerY: Float = 0f
    ): Pair<Float, Float> {
        val x = size * (3f / 2f * coord.q)
        val y = size * (sqrt(3f) / 2f * coord.q + sqrt(3f) * coord.r)
        return (x + centerX) to (y + centerY)
    }

    /**
     * Convert pixel position to the nearest axial hex coordinate.
     *
     * @param pixelX The X pixel position.
     * @param pixelY The Y pixel position.
     * @param size The hex size.
     * @param centerX The pixel X of the board center.
     * @param centerY The pixel Y of the board center.
     * @return The nearest HexCoord.
     */
    fun pixelToHex(
        pixelX: Float,
        pixelY: Float,
        size: Float,
        centerX: Float = 0f,
        centerY: Float = 0f
    ): HexCoord {
        val px = pixelX - centerX
        val py = pixelY - centerY

        val q = (2f / 3f * px) / size
        val r = (-1f / 3f * px + sqrt(3f) / 3f * py) / size

        return axialRound(q, r)
    }

    /**
     * Round fractional axial coordinates to the nearest hex cell.
     * Uses cube coordinate rounding for accuracy.
     */
    fun axialRound(q: Float, r: Float): HexCoord {
        val s = -q - r

        var rq = q.roundToInt()
        var rr = r.roundToInt()
        val rs = s.roundToInt()

        val dq = kotlin.math.abs(rq - q)
        val dr = kotlin.math.abs(rr - r)
        val ds = kotlin.math.abs(rs - s)

        if (dq > dr && dq > ds) {
            rq = -rr - rs
        } else if (dr > ds) {
            rr = -rq - rs
        }
        // else keep rq and rr, rs is recalculated

        return HexCoord(rq, rr)
    }

    /**
     * Get the six corner points of a flat-top hex at the given center.
     *
     * @param centerX Pixel X of hex center.
     * @param centerY Pixel Y of hex center.
     * @param size Hex size (center to corner).
     * @return List of 6 corner (x, y) pairs.
     */
    fun hexCorners(
        centerX: Float,
        centerY: Float,
        size: Float
    ): List<Pair<Float, Float>> {
        return (0 until 6).map { i ->
            val angleDeg = 60f * i
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val x = centerX + size * kotlin.math.cos(angleRad)
            val y = centerY + size * kotlin.math.sin(angleRad)
            x to y
        }
    }

    /**
     * Calculate the hex distance between two coordinates.
     */
    fun distance(a: HexCoord, b: HexCoord): Int = a.distanceTo(b)

    /**
     * Find the nearest playable cell to a pixel position.
     *
     * @param pixelX Pixel X position.
     * @param pixelY Pixel Y position.
     * @param size Hex size.
     * @param centerX Board center X.
     * @param centerY Board center Y.
     * @param playableCells Set of valid cell coordinates.
     * @return The nearest playable HexCoord, or null if none exists.
     */
    fun nearestCell(
        pixelX: Float,
        pixelY: Float,
        size: Float,
        centerX: Float,
        centerY: Float,
        playableCells: Set<HexCoord>,
        maxPixelDistance: Float = size * 1.6f
    ): HexCoord? {
        val candidate = pixelToHex(pixelX, pixelY, size, centerX, centerY)
        if (candidate in playableCells) {
            val (px, py) = hexToPixel(candidate, size, centerX, centerY)
            val distSq = (pixelX - px) * (pixelX - px) + (pixelY - py) * (pixelY - py)
            if (distSq <= maxPixelDistance * maxPixelDistance) {
                return candidate
            }
        }

        return playableCells.filter { cell ->
            val (px, py) = hexToPixel(cell, size, centerX, centerY)
            val distSq = (pixelX - px) * (pixelX - px) + (pixelY - py) * (pixelY - py)
            distSq <= maxPixelDistance * maxPixelDistance
        }.minByOrNull { cell ->
            val (px, py) = hexToPixel(cell, size, centerX, centerY)
            (pixelX - px) * (pixelX - px) + (pixelY - py) * (pixelY - py)
        }
    }

    /**
     * Calculate the optimal hex size to fit a board within the given dimensions.
     *
     * @param boardRadius The board radius in hex rings.
     * @param availableWidth Available pixel width.
     * @param availableHeight Available pixel height.
     * @param padding Padding in pixels around the board.
     * @return The hex size that fits the board.
     */
    fun calculateHexSize(
        boardRadius: Int,
        availableWidth: Float,
        availableHeight: Float,
        padding: Float = 16f
    ): Float {
        val usableWidth = availableWidth - 2 * padding
        val usableHeight = availableHeight - 2 * padding

        // For flat-top hexes:
        // Total width = size * (3 * boardRadius + 1.5)
        // Total height = size * sqrt(3) * (2 * boardRadius + 1)
        val sizeByWidth = usableWidth / (3f * boardRadius + 1.5f)
        val sizeByHeight = usableHeight / (sqrt(3f) * (2f * boardRadius + 1f))

        return minOf(sizeByWidth, sizeByHeight)
    }
}
