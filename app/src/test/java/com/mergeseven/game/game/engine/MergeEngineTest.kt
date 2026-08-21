package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MergeEngineTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var scoreEngine: ScoreEngine
    private lateinit var mergeEngine: MergeEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        scoreEngine = ScoreEngine()
        mergeEngine = MergeEngine(scoreEngine)
    }

    @Test
    fun `three adjacent 4s merge into 8`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // Place three 4s adjacent to each other
        val tiles = listOf(
            Tile(1, 4, HexCoord(0, 0)),
            Tile(2, 4, HexCoord(1, 0)),
            Tile(3, 4, HexCoord(0, 1))
        )
        
        tiles.forEach { board = board.withTile(it) }

        val groups = mergeEngine.findMergeableGroups(board)
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].size)

        val (newBoard, event) = mergeEngine.resolveGroup(board, groups[0])
        
        // Check event
        assertEquals(8, event.resultTile.value)
        assertEquals(3, event.mergedCount)
        
        // Check new board state
        assertEquals(1, newBoard.activeTiles().size)
        assertEquals(8, newBoard.activeTiles()[0].value)
    }

    @Test
    fun `separated 4s do not merge`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // Place 4s that are not adjacent
        val tiles = listOf(
            Tile(1, 4, HexCoord(0, 0)),
            Tile(2, 4, HexCoord(2, 0)),
            Tile(3, 4, HexCoord(0, 2))
        )
        
        tiles.forEach { board = board.withTile(it) }

        val groups = mergeEngine.findMergeableGroups(board)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `connected mixed values do not merge`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // Place connected tiles with mixed values
        val tiles = listOf(
            Tile(1, 4, HexCoord(0, 0)),
            Tile(2, 8, HexCoord(1, 0)),
            Tile(3, 4, HexCoord(0, 1))
        )
        
        tiles.forEach { board = board.withTile(it) }

        // Only two 4s are connected, minimum is 3
        val groups = mergeEngine.findMergeableGroups(board)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `four adjacent 8s merge into 16`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // Place four 8s in a group
        val tiles = listOf(
            Tile(1, 8, HexCoord(0, 0)),
            Tile(2, 8, HexCoord(1, 0)),
            Tile(3, 8, HexCoord(0, 1)),
            Tile(4, 8, HexCoord(1, -1))
        )
        
        tiles.forEach { board = board.withTile(it) }

        val groups = mergeEngine.findMergeableGroups(board)
        assertEquals(1, groups.size)
        assertEquals(4, groups[0].size)

        val (newBoard, event) = mergeEngine.resolveGroup(board, groups[0])
        
        assertEquals(16, event.resultTile.value)
        assertEquals(4, event.mergedCount)
        assertEquals(1, newBoard.activeTiles().size)
    }
}
