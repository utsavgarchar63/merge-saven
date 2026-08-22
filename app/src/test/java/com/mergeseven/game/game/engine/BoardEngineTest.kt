package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.HexCoord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BoardEngineTest {

    private lateinit var boardEngine: BoardEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
    }

    @Test
    fun `test cellCount formula matches generated cells`() {
        // Test radius 0 to 4
        for (r in 0..4) {
            val state = boardEngine.createBoard(radius = r)
            val expectedCount = 3 * r * (r + 1) + 1
            
            assertEquals("Radius $r cell count formula matches", expectedCount, boardEngine.cellCount(r))
            assertEquals("Radius $r actual generated cells", expectedCount, state.totalPlayable)
            assertEquals("Radius $r should be initially empty", expectedCount, state.emptyCells().size)
        }
    }

    @Test
    fun `test default board generation`() {
        val state = boardEngine.createBoard()
        val defaultRadius = Constants.DEFAULT_BOARD_RADIUS
        val expectedCount = 3 * defaultRadius * (defaultRadius + 1) + 1
        
        assertEquals(expectedCount, state.totalPlayable)
        assertTrue(state.isPlayable(HexCoord(0, 0)))
    }

    @Test
    fun `test custom board with blocked cells`() {
        val blocked = setOf(HexCoord(0, 0), HexCoord(1, 0))
        val state = boardEngine.createBoard(radius = 2, blockedCells = blocked)
        
        val expectedTotal = boardEngine.cellCount(2) - blocked.size
        assertEquals(expectedTotal, state.totalPlayable)
        
        assertFalse("Origin should be blocked", state.isPlayable(HexCoord(0, 0)))
        assertFalse("1,0 should be blocked", state.isPlayable(HexCoord(1, 0)))
        assertTrue("-1,0 should be playable", state.isPlayable(HexCoord(-1, 0)))
        
        assertNull("Blocked cells should not be in the cells map", state.tileAt(HexCoord(0, 0)))
    }
}
