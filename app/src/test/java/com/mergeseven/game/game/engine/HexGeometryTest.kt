package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.HexCoord
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class HexGeometryTest {

    @Test
    fun `hex to pixel to hex round trip`() {
        val size = 50f
        val centerX = 500f
        val centerY = 1000f

        val coordsToTest = listOf(
            HexCoord(0, 0),
            HexCoord(1, 0),
            HexCoord(0, 1),
            HexCoord(-1, 1),
            HexCoord(-2, -3),
            HexCoord(5, -5)
        )

        for (coord in coordsToTest) {
            val (px, py) = HexGeometry.hexToPixel(coord, size, centerX, centerY)
            val recoveredCoord = HexGeometry.pixelToHex(px, py, size, centerX, centerY)
            
            assertEquals("Round trip failed for $coord", coord, recoveredCoord)
        }
    }

    @Test
    fun `axial round resolves fractional coordinates correctly`() {
        // Point slightly right of origin should resolve to (0,0)
        assertEquals(HexCoord(0, 0), HexGeometry.axialRound(0.1f, -0.1f))

        // Point near (1,0)
        assertEquals(HexCoord(1, 0), HexGeometry.axialRound(0.9f, -0.1f))
        
        // Point near (0,1)
        assertEquals(HexCoord(0, 1), HexGeometry.axialRound(-0.1f, 0.9f))
    }

    @Test
    fun `hex corners generated correctly`() {
        val size = 100f
        val corners = HexGeometry.hexCorners(0f, 0f, size)
        
        assertEquals(6, corners.size)
        
        // First corner (0 degrees) should be at (size, 0)
        val (x0, y0) = corners[0]
        assertEquals(size, x0, 0.01f)
        assertEquals(0f, y0, 0.01f)
        
        // Fourth corner (180 degrees) should be at (-size, 0)
        val (x3, y3) = corners[3]
        assertEquals(-size, x3, 0.01f)
        assertEquals(0f, y3, 0.01f)
    }
}
