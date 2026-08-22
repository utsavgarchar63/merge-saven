package com.mergeseven.game.game.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardStateTest {

    @Test
    fun `test initial empty board state`() {
        val playable = setOf(HexCoord(0, 0), HexCoord(1, 0))
        val cells = playable.associateWith { null as Tile? }
        
        val state = BoardState(cells, playable)
        
        assertEquals(0, state.occupiedCount)
        assertEquals(2, state.totalPlayable)
        assertEquals(0f, state.fillRatio, 0.001f)
        assertTrue(state.activeTiles().isEmpty())
        assertEquals(2, state.emptyCells().size)
    }

    @Test
    fun `test placing a tile`() {
        val coord = HexCoord(0, 0)
        val playable = setOf(coord)
        val cells = playable.associateWith { null as Tile? }
        val state = BoardState(cells, playable)
        
        val tile = Tile(coord, value = 2)
        val newState = state.withTile(tile)
        
        assertEquals(1, newState.occupiedCount)
        assertEquals(tile, newState.tileAt(coord))
        assertFalse(newState.isEmpty(coord))
        assertTrue(newState.isPlayable(coord))
        assertEquals(1f, newState.fillRatio, 0.001f)
    }

    @Test
    fun `test removing a tile`() {
        val coord = HexCoord(0, 0)
        val tile = Tile(coord, value = 4)
        val playable = setOf(coord)
        val cells = mapOf(coord to tile)
        val state = BoardState(cells, playable)
        
        val newState = state.withoutTile(coord)
        
        assertEquals(0, newState.occupiedCount)
        assertNull(newState.tileAt(coord))
        assertTrue(newState.isEmpty(coord))
    }

    @Test
    fun `test out of bounds coordinates`() {
        val playable = setOf(HexCoord(0, 0))
        val cells = mapOf(HexCoord(0, 0) to null)
        val state = BoardState(cells, playable)
        
        val outOfBounds = HexCoord(5, 5)
        assertFalse(state.isPlayable(outOfBounds))
        assertFalse(state.isEmpty(outOfBounds)) // Not playable, so not "empty playable"
        assertNull(state.tileAt(outOfBounds))
    }
}
