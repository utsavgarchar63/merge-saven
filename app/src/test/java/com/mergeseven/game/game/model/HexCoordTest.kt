package com.mergeseven.game.game.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HexCoordTest {

    @Test
    fun `addition of hex coordinates`() {
        val a = HexCoord(1, -1)
        val b = HexCoord(2, 3)
        assertEquals(HexCoord(3, 2), a + b)
    }

    @Test
    fun `subtraction of hex coordinates`() {
        val a = HexCoord(3, 2)
        val b = HexCoord(1, -1)
        assertEquals(HexCoord(2, 3), a - b)
    }

    @Test
    fun `distance between coordinates`() {
        val origin = HexCoord.ORIGIN
        val neighbor = HexCoord(1, 0)
        val far = HexCoord(2, -3)

        assertEquals(0, origin.distanceTo(origin))
        assertEquals(1, origin.distanceTo(neighbor))
        assertEquals(3, origin.distanceTo(far)) // q=2, r=-3, s=1. max(2,3,1) = 3
    }

    @Test
    fun `six neighbors generated correctly`() {
        val origin = HexCoord.ORIGIN
        val neighbors = origin.neighbors()

        assertEquals(6, neighbors.size)
        // Check East neighbor
        assertEquals(HexCoord(1, 0), neighbors[0])
        // Check all neighbors are distance 1
        neighbors.forEach {
            assertEquals(1, origin.distanceTo(it))
        }
    }

    @Test
    fun `rotate clockwise 60 degrees`() {
        // East -> Southeast
        val east = HexCoord(1, 0)
        assertEquals(HexCoord(0, 1), east.rotateClockwise60())
    }

    @Test
    fun `rotate multiple steps`() {
        val east = HexCoord(1, 0)
        
        // 1 step (60 deg) = Southeast
        assertEquals(HexCoord(0, 1), east.rotate(1))
        
        // 3 steps (180 deg) = West
        assertEquals(HexCoord(-1, 0), east.rotate(3))
        
        // 6 steps (360 deg) = East
        assertEquals(east, east.rotate(6))
        
        // Negative steps (-1 = 5 steps = 300 deg) = Northeast
        assertEquals(HexCoord(1, -1), east.rotate(-1))
    }
}
