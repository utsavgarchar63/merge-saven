package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Af3BoosterEngineTest {

    private val boardEngine = BoardEngine()
    private val engine = GameEngineImpl(
        boardEngine = boardEngine,
        mergeEngine = MergeEngine(ScoreEngine()),
        placementEngine = PlacementEngine(),
        spawnEngine = SpawnEngine(),
        scoreEngine = ScoreEngine(),
        gameOverEngine = GameOverEngine(PlacementEngine()),
        chainReactionEngine = ChainReactionEngine(MergeEngine(ScoreEngine()))
    )

    private fun baseState(board: com.mergeseven.game.game.model.BoardState = boardEngine.createBoard()) =
        GameState.initial(board = board, trayPieces = emptyList())

    @Test
    fun `continue clears lowest tiles and blocks undo`() {
        var board = boardEngine.createBoard()
        val cells = listOf(
            HexCoord(0, 0),
            HexCoord(1, 0),
            HexCoord(2, 0),
            HexCoord(0, 1),
            HexCoord(1, -1),
            HexCoord(-1, 0)
        )
        listOf(2, 4, 8, 16, 32, 64).forEachIndexed { i, value ->
            board = board.withTile(Tile(id = i.toLong(), value = value, cell = cells[i]))
        }
        val prior = baseState()
        val over = baseState(board).copy(
            isGameOver = true,
            previousState = prior,
            continuesUsed = 0
        )

        val result = engine.continueAfterGameOver(over, clearCount = 5, rewarded = false)
        assertFalse(result.state.isGameOver)
        assertNull(result.state.previousState)
        assertTrue(result.state.undoBlockedUntilMove)
        assertEquals(1, result.state.continuesUsed)
        assertEquals(1, result.state.board.activeTiles().size)
        assertEquals(64, result.state.board.activeTiles().first().value)
        assertTrue(result.events.any { it is GameEvent.BoosterActivated && it.type == BoosterType.CONTINUE })
    }

    @Test
    fun `rewarded continue does not bump coin continue count`() {
        val over = baseState().copy(isGameOver = true, continuesUsed = 1, rewardedContinuesUsed = 0)
        val result = engine.continueAfterGameOver(over, clearCount = 0, rewarded = true)
        assertEquals(1, result.state.continuesUsed)
        assertEquals(1, result.state.rewardedContinuesUsed)
    }

    @Test
    fun `time freeze adds frozen ms`() {
        val state = baseState().copy(modeId = ModeIds.TIME_ATTACK, timeRemainingMs = 30_000L)
        val result = engine.timeFreeze(state, Constants.TIME_FREEZE_MS)
        assertEquals(Constants.TIME_FREEZE_MS, result.state.timeFrozenMs)
    }

    @Test
    fun `value up doubles tile up to cap`() {
        val board = boardEngine.createBoard()
            .withTile(Tile(id = 1L, value = 16, cell = HexCoord(0, 0)))
        val state = baseState(board)
        val next = engine.valueUpTile(state, HexCoord(0, 0)).state
        assertEquals(32, next.board.tileAt(HexCoord(0, 0))?.value)
    }

    @Test
    fun `hammer removes targeted tile`() {
        val board = boardEngine.createBoard()
            .withTile(Tile(id = 1L, value = 8, cell = HexCoord(0, 0)))
        val state = baseState(board)
        val next = engine.hammerTile(state, HexCoord(0, 0)).state
        assertTrue(next.board.isEmpty(HexCoord(0, 0)))
    }
}
