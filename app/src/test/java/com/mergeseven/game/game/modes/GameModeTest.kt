package com.mergeseven.game.game.modes

import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.ChainReactionEngine
import com.mergeseven.game.game.engine.GameEngineImpl
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.MergeEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.engine.ScoreEngine
import com.mergeseven.game.game.engine.SpawnEngine
import com.mergeseven.game.game.engine.SpawnHints
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.GameResult
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameModeTest {

    private lateinit var engine: GameEngineImpl
    private lateinit var boardEngine: BoardEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        val score = ScoreEngine()
        val merge = MergeEngine(score)
        val placement = PlacementEngine()
        engine = GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = merge,
            placementEngine = placement,
            spawnEngine = SpawnEngine(),
            scoreEngine = score,
            gameOverEngine = GameOverEngine(placement),
            chainReactionEngine = ChainReactionEngine(merge)
        )
    }

    @Test
    fun `daily seed is stable for a date`() {
        assertEquals(ModeSeeds.dailySeed("2026-09-09"), ModeSeeds.dailySeed("2026-09-09"))
        assertTrue(ModeSeeds.dailySeed("2026-09-09") != ModeSeeds.dailySeed("2026-09-10"))
    }

    @Test
    fun `week key uses ISO week`() {
        val key = ModeSeeds.weekKey("2026-09-09")
        assertTrue(key.contains("W"))
        assertEquals(ModeSeeds.weeklySeed(key), ModeSeeds.weeklySeed(key))
    }

    @Test
    fun `endless escalates spawn tier every 10 moves`() {
        val mode = EndlessMode()
        val state = mode.createSession(
            ModeSessionContext(1, 1L, 0L, gameEngine = engine)
        ).copy(moves = 19)
        val piece = TilePiece(1L, listOf(PieceCell(HexCoord(0, 0), 2)))
        val withTray = state.copy(trayPieces = listOf(piece, null, null))
        val origin = withTray.board.emptyCells().first()
        val result = engine.placePiece(withTray, piece, origin, 0)
        val outcome = mode.afterMove(withTray, result, engine)
        assertEquals(2, outcome.state.spawnTier) // moves become 20 → tier 2
    }

    @Test
    fun `time attack loses when timer hits zero`() {
        val mode = TimeAttackMode()
        val state = mode.createSession(
            ModeSessionContext(1, 1L, 0L, gameEngine = engine)
        ).copy(timeRemainingMs = 500L)
        val outcome = mode.onTimerTick(state, 1_000L, engine)
        assertEquals(ModeEndReason.LOST, outcome.endReason)
        assertEquals(0L, outcome.state.timeRemainingMs)
    }

    @Test
    fun `time attack adds merge time bonus`() {
        val mode = TimeAttackMode()
        val prev = mode.createSession(
            ModeSessionContext(1, 1L, 0L, gameEngine = engine)
        )
        val merge = GameEvent.MergeCompleted(
            resultTile = Tile(1L, 4, HexCoord(0, 0)),
            mergedCount = 3,
            scoreEarned = 10
        )
        val result = GameResult(
            state = prev.copy(moves = 1, score = 10),
            events = listOf(merge)
        )
        val outcome = mode.afterMove(prev, result, engine)
        assertTrue(outcome.state.timeRemainingMs > prev.timeRemainingMs)
    }

    @Test
    fun `zen never marks game over on full board`() {
        val mode = ZenMode()
        var board = boardEngine.createBoard(radius = 2)
        board.playableCells.forEachIndexed { index, coord ->
            board = board.withTile(Tile(index.toLong(), 2, coord))
        }
        val state = GameState.initial(
            board = board,
            trayPieces = listOf(TilePiece(1L, listOf(PieceCell(HexCoord(0, 0), 2)))),
            modeId = ModeIds.ZEN
        )
        val result = GameResult(state = state, events = emptyList())
        val outcome = mode.afterMove(state, result, engine)
        assertFalse(outcome.state.isGameOver)
        assertEquals(null, outcome.endReason)
    }

    @Test
    fun `weekly force triple spawn produces 3-cell pieces`() {
        val spawn = SpawnEngine()
        val random = com.mergeseven.game.game.engine.GameRandom(RngState.fromSeed(42L))
        val piece = spawn.generatePiece(5, random, SpawnHints(forceTriple = true))
        assertEquals(3, piece.cells.size)
    }

    @Test
    fun `campaign win awards stars via onSessionEnded`() {
        val mode = CampaignMode()
        val board = boardEngine.createBoard(radius = 2)
            .withTile(Tile(1L, 16, HexCoord(0, 0)))
        val state = GameState.initial(
            board = board,
            trayPieces = listOf(TilePiece(1L, listOf(PieceCell(HexCoord(0, 0), 2)))),
            targetValue = 16,
            objectives = listOf(
                com.mergeseven.game.game.objectives.LevelObjective.ReachValue("r", 16)
            )
        ).copy(moves = 10)
        val effects = mode.onSessionEnded(state, ModeEndReason.WON)
        assertTrue(effects.starsEarned in 1..3)
    }
}
