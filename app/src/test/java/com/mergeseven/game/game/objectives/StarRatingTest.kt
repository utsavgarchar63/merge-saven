package com.mergeseven.game.game.objectives

import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import org.junit.Assert.assertEquals
import org.junit.Test

class StarRatingTest {

    private val boardEngine = BoardEngine()

    @Test
    fun `returns zero when objectives incomplete`() {
        val state = completeReachState(moves = 10).copy(
            board = boardEngine.createBoard(radius = 2)
                .withTile(Tile(1L, 8, HexCoord(0, 0)))
        )
        assertEquals(0, StarRating.compute(state))
    }

    @Test
    fun `three stars within three-star move cap`() {
        val state = completeReachState(moves = 20, three = 25, two = 40)
        assertEquals(3, StarRating.compute(state))
    }

    @Test
    fun `two stars between three and two caps`() {
        val state = completeReachState(moves = 30, three = 25, two = 40)
        assertEquals(2, StarRating.compute(state))
    }

    @Test
    fun `one star above two-star move cap`() {
        val state = completeReachState(moves = 50, three = 25, two = 40)
        assertEquals(1, StarRating.compute(state))
    }

    private fun completeReachState(
        moves: Int,
        three: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
        two: Int = StarRating.DEFAULT_TWO_STAR_MOVES
    ): GameState {
        val board = boardEngine.createBoard(radius = 2)
            .withTile(Tile(1L, 16, HexCoord(0, 0)))
        return GameState(
            board = board,
            trayPieces = listOf(TilePiece(id = 1L, cells = emptyList())),
            score = 100,
            bestScore = 100,
            coins = 0,
            level = 1,
            targetValue = 16,
            moves = moves,
            isPaused = false,
            isGameOver = false,
            isBusy = false,
            objectives = listOf(LevelObjective.ReachValue("r", 16)),
            threeStarMoveCap = three,
            twoStarMoveCap = two
        )
    }
}
