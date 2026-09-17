package com.mergeseven.game.game.objectives

import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.model.TileTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectiveEvaluatorTest {

    private val boardEngine = BoardEngine()

    @Test
    fun `reach value completes when max tile meets target`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ReachValue("r", 16)),
            tiles = listOf(tile(1, 16, HexCoord(0, 0)))
        )
        assertTrue(ObjectiveEvaluator.progress(state, state.objectives.single()).isComplete)
        assertTrue(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `reach value incomplete below target`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ReachValue("r", 16)),
            tiles = listOf(tile(1, 8, HexCoord(0, 0)))
        )
        assertFalse(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `clear all stone completes with zero stones`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ClearAllStone("c")),
            tiles = listOf(tile(1, 4, HexCoord(0, 0), TileTrait.NORMAL))
        )
        assertTrue(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `clear all stone incomplete while stone remains`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ClearAllStone("c")),
            tiles = listOf(tile(1, 4, HexCoord(0, 0), TileTrait.STONE))
        )
        assertFalse(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `survive moves completes at N without game over`() {
        val state = baseState(
            objectives = listOf(LevelObjective.SurviveMoves("s", 10)),
            moves = 10
        )
        assertTrue(ObjectiveEvaluator.allComplete(state))
        assertFalse(ObjectiveEvaluator.anyFailed(state))
    }

    @Test
    fun `survive moves fails when game over before N`() {
        val state = baseState(
            objectives = listOf(LevelObjective.SurviveMoves("s", 10)),
            moves = 7,
            isGameOver = true
        )
        assertTrue(ObjectiveEvaluator.anyFailed(state))
        assertFalse(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `score in moves completes within budget`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ScoreInMoves("sc", 500, 20)),
            score = 500,
            moves = 12
        )
        assertTrue(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `score in moves fails after budget without score`() {
        val state = baseState(
            objectives = listOf(LevelObjective.ScoreInMoves("sc", 500, 20)),
            score = 100,
            moves = 21
        )
        assertTrue(ObjectiveEvaluator.anyFailed(state))
        assertFalse(ObjectiveEvaluator.allComplete(state))
    }

    @Test
    fun `collect value uses merge counters only`() {
        val objective = LevelObjective.CollectValue("col", tileValue = 8, count = 3)
        val incomplete = baseState(
            objectives = listOf(objective),
            collectedByValue = mapOf(8 to 2)
        )
        assertFalse(ObjectiveEvaluator.allComplete(incomplete))

        val complete = incomplete.copy(collectedByValue = mapOf(8 to 3))
        assertTrue(ObjectiveEvaluator.allComplete(complete))
    }

    @Test
    fun `multi objective requires every objective`() {
        val objectives = listOf(
            LevelObjective.ReachValue("r", 16),
            LevelObjective.ClearAllStone("c")
        )
        val onlyReach = baseState(
            objectives = objectives,
            tiles = listOf(
                tile(1, 16, HexCoord(0, 0)),
                tile(2, 4, HexCoord(1, 0), TileTrait.STONE)
            )
        )
        assertFalse(ObjectiveEvaluator.allComplete(onlyReach))

        val both = onlyReach.copy(
            board = onlyReach.board.withTile(tile(2, 4, HexCoord(1, 0), TileTrait.NORMAL))
        )
        assertTrue(ObjectiveEvaluator.allComplete(both))
    }

    @Test
    fun `empty objectives synthesize reach target`() {
        val state = baseState(objectives = emptyList(), targetValue = 32).copy(
            board = boardEngine.createBoard(radius = 2)
                .withTile(tile(1, 32, HexCoord(0, 0)))
        )
        assertTrue(ObjectiveEvaluator.allComplete(state))
    }

    private fun baseState(
        objectives: List<LevelObjective>,
        tiles: List<Tile> = emptyList(),
        score: Long = 0,
        moves: Int = 0,
        isGameOver: Boolean = false,
        targetValue: Int = 16,
        collectedByValue: Map<Int, Int> = emptyMap()
    ): GameState {
        var board = boardEngine.createBoard(radius = 2)
        tiles.forEach { board = board.withTile(it) }
        return GameState(
            board = board,
            trayPieces = listOf(TilePiece(id = 99L, cells = emptyList())),
            score = score,
            bestScore = score,
            coins = 0,
            level = 1,
            targetValue = targetValue,
            moves = moves,
            isPaused = false,
            isGameOver = isGameOver,
            isBusy = false,
            objectives = objectives,
            collectedByValue = collectedByValue
        )
    }

    private fun tile(
        id: Long,
        value: Int,
        cell: HexCoord,
        trait: TileTrait = TileTrait.NORMAL
    ) = Tile(id = id, value = value, cell = cell, trait = trait)
}
