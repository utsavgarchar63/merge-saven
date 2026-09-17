package com.mergeseven.game.data.local.snapshot

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateSnapshotTest {

    private val boardEngine = BoardEngine()

    @Test
    fun `round trip through json preserves the whole game`() {
        val original = gameState()

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(original, restored)
    }

    @Test
    fun `round trip preserves tile positions and values`() {
        val original = gameState()

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(original.board.activeTiles().toSet(), restored.board.activeTiles().toSet())
        assertEquals(original.board.playableCells, restored.board.playableCells)
        assertEquals(original.board.emptyCells(), restored.board.emptyCells())
    }

    @Test
    fun `round trip preserves the tray including empty slots`() {
        val original = gameState().copy(
            trayPieces = listOf(piece(1L), null, piece(3L))
        )

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(3, restored.trayPieces.size)
        assertNull(restored.trayPieces[1])
        assertEquals(original.trayPieces, restored.trayPieces)
    }

    @Test
    fun `undo chain is capped so a long run cannot grow the save without bound`() {
        val deepChain = (1..10).fold(gameState()) { previous, index ->
            gameState(score = index * 100L).copy(previousState = previous)
        }

        val snapshot = deepChain.toSnapshot()

        assertEquals(Constants.MAX_UNDO_HISTORY, undoDepth(snapshot))
    }

    @Test
    fun `undo chain survives the round trip`() {
        val withHistory = gameState(score = 500L).copy(previousState = gameState(score = 200L))

        val restored = decode(encode(withHistory.toSnapshot()))

        assertNotNull(restored.previousState)
        assertEquals(200L, restored.previousState?.score)
    }

    @Test
    fun `transient ui flags are not restored`() {
        val paused = gameState().copy(isPaused = true, isBusy = true)

        val restored = decode(encode(paused.toSnapshot()))

        assertTrue("a resumed game should not come back paused", !restored.isPaused)
        assertTrue("a resumed game should not come back busy", !restored.isBusy)
    }

    @Test
    fun `game over state is restored`() {
        val finished = gameState().copy(isGameOver = true)

        val restored = decode(encode(finished.toSnapshot()))

        assertTrue(restored.isGameOver)
    }

    @Test
    fun `the random cursor is restored so the piece stream continues`() {
        val original = gameState().copy(
            rng = RngState(seed = 99L, cursor = -12345L, draws = 41, nextEntityId = 77L)
        )

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(original.rng, restored.rng)
    }

    @Test
    fun `cell modifiers survive a snapshot round trip`() {
        val board = boardEngine.createBoard(radius = 2).withModifiers(
            mapOf(
                HexCoord(0, 0) to com.mergeseven.game.game.model.CellModifier(
                    type = com.mergeseven.game.game.model.CellModifierType.SCORE_PAD,
                    scoreBonus = 2f
                ),
                HexCoord(1, 0) to com.mergeseven.game.game.model.CellModifier(
                    type = com.mergeseven.game.game.model.CellModifierType.LOCKED
                )
            )
        )
        val original = gameState().copy(board = board)

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(original.board.cellModifiers, restored.board.cellModifiers)
    }

    @Test
    fun `objectives and collect map survive snapshot round trip`() {
        val original = gameState().copy(
            objectives = listOf(
                com.mergeseven.game.game.objectives.LevelObjective.ReachValue("reach64", 64),
                com.mergeseven.game.game.objectives.LevelObjective.CollectValue(
                    id = "collect8",
                    tileValue = 8,
                    count = 5
                )
            ),
            collectedByValue = mapOf(8 to 3, 16 to 1),
            threeStarMoveCap = 20,
            twoStarMoveCap = 35
        )

        val restored = decode(encode(original.toSnapshot()))

        assertEquals(original.objectives, restored.objectives)
        assertEquals(original.collectedByValue, restored.collectedByValue)
        assertEquals(20, restored.threeStarMoveCap)
        assertEquals(35, restored.twoStarMoveCap)
    }

    @Test
    fun `empty objectives restore as reach target`() {
        val original = gameState().copy(objectives = emptyList(), targetValue = 128)
        val snapshot = original.toSnapshot().copy(objectives = emptyList())
        val restored = snapshot.toGameState()

        assertEquals(1, restored.objectives.size)
        val reach = restored.objectives.single() as com.mergeseven.game.game.objectives.LevelObjective.ReachValue
        assertEquals(128, reach.value)
    }

    @Test
    fun `unknown fields in stored json are ignored`() {
        val json = encode(gameState().toSnapshot())
            .replaceFirst("{", """{"someFieldFromAFutureRelease":42,""")

        val restored = decode(json)

        assertEquals(1_234L, restored.score)
    }

    private fun encode(snapshot: GameStateSnapshot): String =
        GameStateSnapshot.json.encodeToString(GameStateSnapshot.serializer(), snapshot)

    private fun decode(json: String): GameState =
        GameStateSnapshot.json
            .decodeFromString(GameStateSnapshot.serializer(), json)
            .toGameState()

    private fun undoDepth(snapshot: GameStateSnapshot): Int {
        var depth = 0
        var current = snapshot.previous
        while (current != null) {
            depth++
            current = current.previous
        }
        return depth
    }

    private fun gameState(score: Long = 1_234L): GameState {
        val board = boardEngine.createBoard(radius = 2)
            .withTile(Tile(id = 1L, value = 4, cell = HexCoord(0, 0)))
            .withTile(Tile(id = 2L, value = 8, cell = HexCoord(1, -1)))
            .withTile(Tile(id = 3L, value = 4, cell = HexCoord(-1, 1)))

        return GameState(
            board = board,
            trayPieces = listOf(piece(10L), piece(11L), piece(12L)),
            score = score,
            bestScore = 9_000L,
            coins = 250,
            level = 3,
            targetValue = 64,
            moves = 17,
            isPaused = false,
            isGameOver = false,
            isBusy = false,
            objectives = listOf(
                com.mergeseven.game.game.objectives.LevelObjective.ReachValue(
                    id = "reach_target",
                    value = 64
                )
            )
        )
    }

    private fun piece(id: Long) = TilePiece(
        id = id,
        cells = listOf(
            PieceCell(HexCoord(0, 0), 2),
            PieceCell(HexCoord(1, 0), 4)
        ),
        rotation = 2
    )
}
