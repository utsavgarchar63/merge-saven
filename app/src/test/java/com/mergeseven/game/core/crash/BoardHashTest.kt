package com.mergeseven.game.core.crash

import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BoardHashTest {

    @Test
    fun sameBoardSameHash() {
        val board = BoardEngine().createBoard()
            .withTile(Tile(1L, 4, HexCoord(0, 0)))
            .withTile(Tile(2L, 8, HexCoord(1, -1)))
        assertEquals(BoardHash.of(board), BoardHash.of(board))
    }

    @Test
    fun differentLayoutDifferentHash() {
        val a = BoardEngine().createBoard().withTile(Tile(1L, 4, HexCoord(0, 0)))
        val b = BoardEngine().createBoard().withTile(Tile(1L, 8, HexCoord(0, 0)))
        assertNotEquals(BoardHash.of(a), BoardHash.of(b))
    }
}
