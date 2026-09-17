package com.mergeseven.game.game.solver

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.ChainReactionEngine
import com.mergeseven.game.game.engine.GameEngineImpl
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.MergeEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.engine.ScoreEngine
import com.mergeseven.game.game.engine.SpawnEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.modes.ModeSeeds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEvaluatorTest {

    private val evaluator = BoardEvaluator()
    private val boardEngine = BoardEngine()

    @Test
    fun `empty board scores higher than nearly full board`() {
        val empty = boardEngine.createBoard(radius = 2)
        var full = empty
        empty.playableCells.forEachIndexed { index, cell ->
            if (index < empty.playableCells.size - 1) {
                // Distinct values avoid cluster bonus dominating open-cell score.
                val value = listOf(2, 4, 8, 16, 32)[index % 5]
                full = full.withTile(Tile(id = index.toLong(), value = value, cell = cell))
            }
        }
        val emptyScore = evaluator.scoreBoard(empty)
        val fullScore = evaluator.scoreBoard(full)
        assertTrue(emptyScore > fullScore)
    }

    @Test
    fun `clustered same values score higher than isolated`() {
        var clustered = boardEngine.createBoard(radius = 2)
        clustered = clustered.withTile(Tile(1, 8, HexCoord(0, 0)))
        clustered = clustered.withTile(Tile(2, 8, HexCoord(1, 0)))
        clustered = clustered.withTile(Tile(3, 8, HexCoord(0, 1)))

        var isolated = boardEngine.createBoard(radius = 2)
        isolated = isolated.withTile(Tile(1, 8, HexCoord(0, 0)))
        isolated = isolated.withTile(Tile(2, 8, HexCoord(2, -2)))
        isolated = isolated.withTile(Tile(3, 8, HexCoord(-2, 2)))

        assertTrue(
            evaluator.scoreBoard(clustered) > evaluator.scoreBoard(isolated)
        )
    }
}

class MoveSolverTest {

    private val boardEngine = BoardEngine()
    private val placement = PlacementEngine()
    private val scoreEngine = ScoreEngine()
    private val mergeEngine = MergeEngine(scoreEngine)
    private val engine = GameEngineImpl(
        boardEngine = boardEngine,
        mergeEngine = mergeEngine,
        placementEngine = placement,
        spawnEngine = SpawnEngine(),
        scoreEngine = scoreEngine,
        gameOverEngine = GameOverEngine(placement),
        chainReactionEngine = ChainReactionEngine(mergeEngine)
    )
    private val solver = MoveSolver(engine, placement, BoardEvaluator())

    @Test
    fun `returns legal hint for open board with single tray piece`() {
        val board = boardEngine.createBoard(radius = 2)
        val piece = TilePiece(
            id = 1L,
            cells = listOf(PieceCell(HexCoord(0, 0), 2))
        )
        val state = GameState.initial(board, listOf(piece))
        val hint = solver.findBestMove(state)
        assertNotNull(hint)
        val rotated = piece.copy(rotation = hint!!.rotation)
        assertTrue(engine.canPlace(state, rotated, hint.origin))
    }

    @Test
    fun `returns null when tray cannot place`() {
        var board = boardEngine.createBoard(radius = 1)
        board.playableCells.forEachIndexed { i, cell ->
            board = board.withTile(Tile(i.toLong(), 4, cell))
        }
        val piece = TilePiece(id = 9L, cells = listOf(PieceCell(HexCoord(0, 0), 2)))
        val state = GameState.initial(board, listOf(piece))
        assertNull(solver.findBestMove(state))
        assertNull(solver.findAnyLegalMove(state))
    }
}

class DeadlockPredictorTest {

    private val boardEngine = BoardEngine()
    private val placement = PlacementEngine()
    private val predictor = DeadlockPredictor(placement, GameOverEngine(placement))

    @Test
    fun `near deadlock when only one origin remains`() {
        var board = boardEngine.createBoard(radius = 1)
        val cells = board.playableCells.toList()
        // Fill all but one cell
        cells.drop(1).forEachIndexed { i, cell ->
            board = board.withTile(Tile(i.toLong(), 2, cell))
        }
        val piece = TilePiece(id = 1L, cells = listOf(PieceCell(HexCoord(0, 0), 4)))
        val state = GameState.initial(board, listOf(piece))
        assertTrue(predictor.minLegalPlacements(state) <= 1)
        assertTrue(predictor.isNearDeadlock(state))
    }
}

class HintSearchBudgetTest {

    @Test
    fun `expires after budget`() {
        var now = 1000L
        val budget = HintSearchBudget(30L) { now }
        assertTrue(budget.hasTime())
        now = 1040L
        assertTrue(budget.expired())
    }
}

class DailySeedValidatorTest {

    private val boardEngine = BoardEngine()
    private val placement = PlacementEngine()
    private val scoreEngine = ScoreEngine()
    private val mergeEngine = MergeEngine(scoreEngine)
    private val engine = GameEngineImpl(
        boardEngine = boardEngine,
        mergeEngine = mergeEngine,
        placementEngine = placement,
        spawnEngine = SpawnEngine(),
        scoreEngine = scoreEngine,
        gameOverEngine = GameOverEngine(placement),
        chainReactionEngine = ChainReactionEngine(mergeEngine)
    )
    private val validator = DailySeedValidator(
        gameEngine = engine,
        moveSolver = MoveSolver(engine, placement, BoardEvaluator())
    )

    @Test
    fun `resolveSeed is deterministic for a date`() {
        val a = validator.resolveSeed("2026-09-09", targetScore = 500, maxMoves = 20)
        val b = validator.resolveSeed("2026-09-09", targetScore = 500, maxMoves = 20)
        assertEquals(a, b)
    }

    @Test
    fun `raw daily seed unchanged without AF4 path`() {
        assertEquals(
            ModeSeeds.dailySeed("2026-01-01"),
            ModeSeeds.dailySeedResolved("2026-01-01", 3000, resolver = null)
        )
    }
}

class SpawnBoostTest {

    @Test
    fun `board value boost changes weight distribution`() {
        val spawn = SpawnEngine()
        val base = spawn.weightsForTier(0)
        val boosted = base.toMutableMap()
        boosted[2] = (boosted[2] ?: 0) + Constants.ADAPTIVE_SPAWN_BOOST
        assertTrue(boosted[2]!! > base[2]!!)
    }
}

class CachedMoveSolverTest {

    @Test
    fun `cache returns same hint for same state`() = runTest {
        val boardEngine = BoardEngine()
        val placement = PlacementEngine()
        val scoreEngine = ScoreEngine()
        val mergeEngine = MergeEngine(scoreEngine)
        val engine = GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = mergeEngine,
            placementEngine = placement,
            spawnEngine = SpawnEngine(),
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placement),
            chainReactionEngine = ChainReactionEngine(mergeEngine)
        )
        val cached = CachedMoveSolver(
            MoveSolver(engine, placement, BoardEvaluator()),
            object : com.mergeseven.game.core.DispatcherProvider {
                override val main = kotlinx.coroutines.Dispatchers.Unconfined
                override val io = kotlinx.coroutines.Dispatchers.Unconfined
                override val default = kotlinx.coroutines.Dispatchers.Unconfined
                override val unconfined = kotlinx.coroutines.Dispatchers.Unconfined
            }
        )
        val piece = TilePiece(1L, listOf(PieceCell(HexCoord(0, 0), 2)))
        val state = GameState.initial(boardEngine.createBoard(radius = 2), listOf(piece))
        val first = cached.findBest(state, DifficultyProfiles.STANDARD)
        val second = cached.peekCached(state)
        assertEquals(first, second)
        assertFalse(first == null && second != null)
    }
}
