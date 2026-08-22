package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChainReactionEngineTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var scoreEngine: ScoreEngine
    private lateinit var mergeEngine: MergeEngine
    private lateinit var chainReactionEngine: ChainReactionEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        scoreEngine = ScoreEngine()
        mergeEngine = MergeEngine(scoreEngine)
        chainReactionEngine = ChainReactionEngine(mergeEngine)
    }

    @Test
    fun `test a 2-step chain reaction`() {
        var board = boardEngine.createBoard(radius = 3)
        
        // Setup:
        // We want three 4s to merge into an 8.
        // We will force the destination to be HexCoord(0,0) by making it the center.
        // Then we place two other 8s adjacent to (0,0) so the resulting 8 triggers a second merge into 16.
        val tiles = listOf(
            Tile(1, 4, HexCoord(0, 0)),
            Tile(2, 4, HexCoord(1, 0)),
            Tile(3, 4, HexCoord(0, 1)),
            Tile(4, 8, HexCoord(-1, 0)),
            Tile(5, 8, HexCoord(0, -1))
        )
        
        tiles.forEach { board = board.withTile(it) }

        // Start chain reaction
        val result = chainReactionEngine.resolveChains(board, recentlyPlaced = listOf(tiles[0]))

        // The first step merges the three 4s into an 8 at (0,0).
        // The second step merges the new 8 at (0,0) with the existing 8s at (-1,0) and (0,-1) into a 16.
        
        // Chain length should be 2
        assertEquals("Chain length should be exactly 2", 2, result.chainLength)
        
        // Resulting board should have exactly one tile (value 16)
        val activeTiles = result.finalBoard.activeTiles()
        assertEquals(1, activeTiles.size)
        assertEquals(16, activeTiles[0].value)
    }

    @Test
    fun `test a 3-step chain reaction`() {
        var board = boardEngine.createBoard(radius = 4)
        
        // Setup a cascade: 2s -> 4s -> 8s -> 16s
        // We place three 2s near the center.
        val t1 = Tile(1, 2, HexCoord(0, 0))
        val t2 = Tile(2, 2, HexCoord(1, 0))
        val t3 = Tile(3, 2, HexCoord(0, 1))
        
        // Two existing 4s adjacent to (0,0)
        val t4 = Tile(4, 4, HexCoord(-1, 0))
        val t5 = Tile(5, 4, HexCoord(0, -1))
        
        // Two existing 8s adjacent to (-1,0) (where the 4 will likely resolve)
        val t6 = Tile(6, 8, HexCoord(-2, 0))
        val t7 = Tile(7, 8, HexCoord(-1, -1))
        
        val tiles = listOf(t1, t2, t3, t4, t5, t6, t7)
        tiles.forEach { board = board.withTile(it) }

        val result = chainReactionEngine.resolveChains(board, recentlyPlaced = listOf(t1))

        // Step 1: 2s merge into 4 at (0,0).
        // Step 2: New 4 at (0,0) merges with (-1,0) and (0,-1) into an 8.
        // Step 3: New 8 merges with existing 8s into a 16.
        
        assertEquals(3, result.chainLength)
        val activeTiles = result.finalBoard.activeTiles()
        assertEquals(1, activeTiles.size)
        assertEquals(16, activeTiles[0].value)
    }
}
