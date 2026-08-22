package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameOverEngineTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var placementEngine: PlacementEngine
    private lateinit var gameOverEngine: GameOverEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        placementEngine = PlacementEngine()
        gameOverEngine = GameOverEngine(placementEngine)
    }

    @Test
    fun `test empty board is not game over`() {
        val board = boardEngine.createBoard(radius = 2)
        val state = GameState.initial(
            board = board,
            currentPiece = TilePiece(
                id = 1,
                cells = listOf(TilePiece.PieceCell(1, 4, HexCoord(0, 0)))
            ),
            nextPieces = emptyList(),
            level = 1,
            bestScore = 0
        )
        assertFalse("Empty board should never be game over", gameOverEngine.isGameOver(state))
    }

    @Test
    fun `test completely full board is game over`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // Fill every playable cell
        board.playableCells.forEachIndexed { index, coord ->
            board = board.withTile(Tile(index.toLong(), 2, coord))
        }

        val state = GameState.initial(
            board = board,
            currentPiece = TilePiece(
                id = 1,
                cells = listOf(TilePiece.PieceCell(1, 4, HexCoord(0, 0)))
            ),
            nextPieces = emptyList(),
            level = 1,
            bestScore = 0
        )
        
        assertTrue("Completely full board must trigger game over", gameOverEngine.isGameOver(state))
    }

    @Test
    fun `test no valid placement despite empty cells edge case`() {
        var board = boardEngine.createBoard(radius = 2)
        
        // A radius 2 board has 19 cells.
        // We will leave exactly 1 cell empty: HexCoord(0,0)
        board.playableCells.filter { it != HexCoord(0,0) }.forEachIndexed { index, coord ->
            board = board.withTile(Tile(index.toLong(), 2, coord))
        }
        
        // Now the board has 1 empty cell.
        // If the piece requires 2 cells, it cannot fit anywhere.
        val piece = TilePiece(
            id = 1,
            cells = listOf(
                TilePiece.PieceCell(1, 4, HexCoord(0, 0)),
                TilePiece.PieceCell(2, 4, HexCoord(1, 0))
            )
        )
        
        val state = GameState.initial(
            board = board,
            currentPiece = piece,
            nextPieces = emptyList(),
            level = 1,
            bestScore = 0
        )
        
        // Even though there is an empty cell, the piece cannot fit, so game over!
        assertTrue("Game over should trigger when piece can't fit in remaining isolated cells", gameOverEngine.isGameOver(state))
    }
}
