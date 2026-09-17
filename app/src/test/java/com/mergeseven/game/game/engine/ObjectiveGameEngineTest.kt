package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.modes.CampaignMode
import com.mergeseven.game.game.modes.ModeEndReason
import com.mergeseven.game.game.objectives.LevelObjective
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObjectiveGameEngineTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var gameEngine: GameEngineImpl
    private val campaignMode = CampaignMode()

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        val scoreEngine = ScoreEngine()
        val mergeEngine = MergeEngine(scoreEngine)
        val placement = PlacementEngine()
        gameEngine = GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = mergeEngine,
            placementEngine = placement,
            spawnEngine = SpawnEngine(),
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placement),
            chainReactionEngine = ChainReactionEngine(mergeEngine)
        )
    }

    @Test
    fun `emits LevelCompleted only when all objectives done`() {
        val reachAndSurvive = listOf(
            LevelObjective.ReachValue("r", 4),
            LevelObjective.SurviveMoves("s", 5)
        )
        val board = boardEngine.createBoard(radius = 2)
            .withTile(Tile(1L, 4, HexCoord(0, 0)))
        val state = GameState.initial(
            board = board,
            trayPieces = listOf(single(2L, 2)),
            targetValue = 4,
            objectives = reachAndSurvive
        ).copy(moves = 4)

        val empty = board.emptyCells().first { it != HexCoord(0, 0) }
        val result = gameEngine.placePiece(state, state.trayPieces[0]!!, empty, 0)
        val outcome = campaignMode.afterMove(state, result, gameEngine)

        assertTrue(outcome.endReason == ModeEndReason.WON)
        assertTrue(outcome.events.any { it is GameEvent.LevelCompleted })
        assertTrue(outcome.state.moves >= 5)
    }

    @Test
    fun `score in moves over budget emits GameOver`() {
        val objectives = listOf(LevelObjective.ScoreInMoves("sc", 10_000, 2))
        val board = boardEngine.createBoard(radius = 2)
        val state = GameState.initial(
            board = board,
            trayPieces = listOf(single(2L, 2)),
            objectives = objectives
        ).copy(moves = 2, score = 0)

        val origin = board.emptyCells().first()
        val result = gameEngine.placePiece(state, state.trayPieces[0]!!, origin, 0)
        val outcome = campaignMode.afterMove(state, result, gameEngine)

        assertTrue(outcome.state.isGameOver)
        assertTrue(outcome.endReason == ModeEndReason.LOST)
        assertTrue(outcome.events.any { it is GameEvent.GameOver })
        assertFalse(outcome.events.any { it is GameEvent.LevelCompleted })
    }

    @Test
    fun `collect increments only for merge result values`() {
        val board = boardEngine.createBoard(radius = 2)
            .withTile(Tile(1L, 2, HexCoord(0, 0)))
            .withTile(Tile(2L, 2, HexCoord(1, 0)))
        val piece = TilePiece(
            id = 10L,
            cells = listOf(PieceCell(HexCoord(0, 0), 2))
        )
        val state = GameState.initial(
            board = board,
            trayPieces = listOf(piece),
            objectives = listOf(LevelObjective.CollectValue("c", tileValue = 4, count = 1)),
            rng = RngState.fromSeed(1L)
        )
        val result = gameEngine.placePiece(state, piece, HexCoord(0, 1), 0)

        assertTrue(
            "expected collect of 4 from merge, got ${result.state.collectedByValue}",
            (result.state.collectedByValue[4] ?: 0) >= 1
        )
    }

    private fun single(id: Long, value: Int) = TilePiece(
        id = id,
        cells = listOf(PieceCell(HexCoord(0, 0), value))
    )
}
