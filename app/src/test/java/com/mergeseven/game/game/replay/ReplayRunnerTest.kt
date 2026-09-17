package com.mergeseven.game.game.replay

import com.mergeseven.game.data.local.snapshot.toGameState
import com.mergeseven.game.data.local.snapshot.toSnapshot
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.ChainReactionEngine
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.engine.GameEngineImpl
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.MergeEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.engine.ScoreEngine
import com.mergeseven.game.game.engine.SpawnEngine
import com.mergeseven.game.game.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These are the tests that would fail if anyone reintroduced `Random.Default` or `System.nanoTime()`
 * into the engine, so they are the guard on the determinism guarantee as a whole.
 */
class ReplayRunnerTest {

    private val engine: GameEngine = newEngine()
    private val runner = ReplayRunner(engine)

    @Test
    fun `the same seed and actions produce an identical final state`() {
        val replay = recordRun(seed = 1234L, level = 1, moves = 12)

        val first = runner.run(replay).finalState
        val second = ReplayRunner(newEngine()).run(replay).finalState

        assertEquals(first, second)
    }

    @Test
    fun `identical means identical down to tile ids`() {
        val replay = recordRun(seed = 555L, level = 2, moves = 8)

        val first = runner.run(replay).finalState.board.activeTiles()
        val second = ReplayRunner(newEngine()).run(replay).finalState.board.activeTiles()

        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `a different seed produces a different run`() {
        val actions = recordRun(seed = 1234L, level = 1, moves = 12).actions

        val first = runner.run(Replay(seed = 1234L, level = 1, actions = actions)).finalState
        val second = runner.run(Replay(seed = 9999L, level = 1, actions = actions)).finalState

        assertNotEquals(first.board.activeTiles().toSet(), second.board.activeTiles().toSet())
    }

    @Test
    fun `a new game with no actions is already reproducible`() {
        val first = engine.createInitialState(level = 3, seed = 77L)
        val second = engine.createInitialState(level = 3, seed = 77L)

        assertEquals(first, second)
    }

    @Test
    fun `two fresh games without a seed differ`() {
        val first = engine.createInitialState(level = 3)
        val second = engine.createInitialState(level = 3)

        assertNotEquals(first.rng.seed, second.rng.seed)
    }

    @Test
    fun `a faithful replay skips nothing`() {
        val replay = recordRun(seed = 31L, level = 1, moves = 10)

        val result = runner.run(replay)

        assertTrue("replay diverged from the engine", result.isFaithful)
        assertEquals(replay.actions.size, result.applied)
    }

    @Test
    fun `an illegal action is skipped instead of throwing`() {
        val replay = recordRun(seed = 31L, level = 1, moves = 4).let { recorded ->
            recorded.copy(
                actions = recorded.actions + ReplayAction.Place(
                    slotIndex = 0,
                    origin = com.mergeseven.game.game.model.HexCoord(99, 99)
                )
            )
        }

        val result = runner.run(replay)

        assertEquals(1, result.skipped)
        assertTrue(!result.isFaithful)
    }

    @Test
    fun `a replay survives a round trip through json`() {
        val replay = recordRun(seed = 8080L, level = 2, moves = 6)

        val restored = Replay.decode(replay.encode())

        assertEquals(replay, restored)
        assertEquals(runner.run(replay).finalState, runner.run(restored).finalState)
    }

    @Test
    fun `resuming from a save continues the same piece stream`() {
        // The AF0-14 payoff: play, save, reload, and keep playing must match never having stopped.
        val replay = recordRun(seed = 4242L, level = 1, moves = 6)
        val uninterrupted = runner.run(replay).finalState

        val halfway = runner.run(replay.copy(actions = replay.actions.take(3))).finalState
        val reloaded = halfway.toSnapshot().toGameState()
        val afterReload = replay.actions.drop(3).fold(reloaded) { state, action ->
            applyOrSame(state, action)
        }

        assertEquals(uninterrupted.board.activeTiles().toSet(), afterReload.board.activeTiles().toSet())
        assertEquals(uninterrupted.trayPieces, afterReload.trayPieces)
        assertEquals(uninterrupted.score, afterReload.score)
        assertEquals(uninterrupted.rng, afterReload.rng)
    }

    @Test
    fun `undo rewinds the random sequence so the same move repeats`() {
        val state = engine.createInitialState(level = 1, seed = 66L)
        val piece = state.trayPieces.first()!!
        val origin = firstValidOrigin(state, 0)!!

        val afterMove = engine.placePiece(state, piece, origin, 0).state
        val afterUndo = engine.undo(afterMove)
        val redone = engine.placePiece(afterUndo, piece, origin, 0).state

        assertEquals(afterMove, redone)
    }

    /**
     * Plays a run by always taking the first legal placement, which gives a deterministic action
     * list without hand-authoring one.
     */
    private fun recordRun(seed: Long, level: Int, moves: Int): Replay {
        var state = engine.createInitialState(level = level, seed = seed)
        val actions = mutableListOf<ReplayAction>()

        repeat(moves) {
            val slot = state.trayPieces.indexOfFirst { it != null }
            if (slot < 0) return@repeat
            val origin = firstValidOrigin(state, slot) ?: return@repeat

            actions += ReplayAction.Place(slot, origin)
            state = engine.placePiece(state, state.trayPieces[slot]!!, origin, slot).state
        }

        return Replay(seed = seed, level = level, actions = actions)
    }

    private fun firstValidOrigin(state: GameState, slot: Int) =
        state.trayPieces.getOrNull(slot)?.let { piece ->
            state.board.playableCells
                .sortedWith(compareBy({ it.q }, { it.r }))
                .firstOrNull { engine.canPlace(state, piece, it) }
        }

    private fun applyOrSame(state: GameState, action: ReplayAction): GameState =
        when (action) {
            is ReplayAction.Place -> {
                val piece = state.trayPieces.getOrNull(action.slotIndex)
                if (piece != null && engine.canPlace(state, piece, action.origin)) {
                    engine.placePiece(state, piece, action.origin, action.slotIndex).state
                } else {
                    state
                }
            }

            else -> state
        }

    private fun newEngine(): GameEngine {
        val scoreEngine = ScoreEngine()
        val mergeEngine = MergeEngine(scoreEngine)
        val placementEngine = PlacementEngine()
        return GameEngineImpl(
            boardEngine = BoardEngine(),
            mergeEngine = mergeEngine,
            placementEngine = placementEngine,
            spawnEngine = SpawnEngine(),
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placementEngine),
            chainReactionEngine = ChainReactionEngine(mergeEngine)
        )
    }
}
