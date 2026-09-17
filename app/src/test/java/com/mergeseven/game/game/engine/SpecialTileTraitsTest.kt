package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TileTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpecialTileTraitsTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var mergeEngine: MergeEngine
    private lateinit var chainEngine: ChainReactionEngine
    private lateinit var gameEngine: GameEngine

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        val scoreEngine = ScoreEngine()
        mergeEngine = MergeEngine(scoreEngine)
        chainEngine = ChainReactionEngine(mergeEngine)
        val placement = PlacementEngine()
        gameEngine = GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = mergeEngine,
            placementEngine = placement,
            spawnEngine = SpawnEngine(),
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placement),
            chainReactionEngine = chainEngine
        )
    }

    // ── Wildcard ──────────────────────────────────────────────

    @Test
    fun `wildcard joins a same-value group of two to make three`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.normal(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.wildcard(3, 2, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].size)

        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(8, resolution.event.resultTile.value)
    }

    @Test
    fun `all-wildcard cluster of three merges using first value doubled`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.wildcard(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.wildcard(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.wildcard(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        assertEquals(1, groups.size)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(8, resolution.event.resultTile.value)
    }

    @Test
    fun `wildcard does not bridge two different non-wildcard values`() {
        var board = boardEngine.createBoard(radius = 2)
        // 4 - W - 8 in a line
        board = board.withTile(Tile.normal(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.wildcard(2, 2, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 8, HexCoord(2, 0)))

        val groups = mergeEngine.findMergeableGroups(board)
        assertTrue("4+W or 8+W alone are size 2", groups.isEmpty())
    }

    // ── Bomb ──────────────────────────────────────────────────

    @Test
    fun `bomb merge clears its six neighbours`() {
        var board = boardEngine.createBoard(radius = 2)
        val bombCell = HexCoord(0, 0)
        board = board.withTile(Tile.bomb(1, 4, bombCell))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        // Neighbour that should be cleared (not in merge group)
        board = board.withTile(Tile.normal(4, 8, HexCoord(-1, 0)))

        val result = chainEngine.resolveChains(board, testRandom())
        assertTrue(result.finalBoard.tileAt(HexCoord(-1, 0)) == null)
        assertTrue(result.events.any { it is GameEvent.TilesCleared })
    }

    @Test
    fun `bomb merge scores at reduced rate`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.bomb(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        val base = 8L * 3L
        val expected = (base * Constants.BOMB_SCORE_FACTOR).toLong()
        assertEquals(expected, resolution.event.scoreEarned)
    }

    @Test
    fun `bomb clear does not chain-explode another bomb`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.bomb(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        // Adjacent bomb sitting on a neighbour cell — cleared, but its AOE must not fire
        board = board.withTile(Tile.bomb(4, 16, HexCoord(-1, 0)))
        board = board.withTile(Tile.normal(5, 32, HexCoord(-2, 0)))

        val result = chainEngine.resolveChains(board, testRandom())
        assertTrue(result.finalBoard.tileAt(HexCoord(-1, 0)) == null)
        assertTrue(
            "neighbour of the cleared bomb must survive",
            result.finalBoard.tileAt(HexCoord(-2, 0)) != null
        )
    }

    // ── Frozen ────────────────────────────────────────────────

    @Test
    fun `frozen tiles are excluded from merge groups`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.frozen(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        assertTrue(mergeEngine.findMergeableGroups(board).isEmpty())
    }

    @Test
    fun `adjacent merge thaws frozen by one stage`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.normal(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.frozen(4, 8, HexCoord(-1, 0), stage = 2))

        val result = chainEngine.resolveChains(board, testRandom())
        val frozen = result.finalBoard.tileAt(HexCoord(-1, 0))
        assertEquals(TileTrait.FROZEN, frozen?.trait)
        assertEquals(1, frozen?.freezeStage)
    }

    @Test
    fun `second adjacent merge fully thaws frozen to normal`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.normal(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.frozen(4, 8, HexCoord(-1, 0), stage = 1))

        val result = chainEngine.resolveChains(board, testRandom())
        val tile = result.finalBoard.tileAt(HexCoord(-1, 0))
        assertEquals(TileTrait.NORMAL, tile?.trait)
        assertEquals(0, tile?.freezeStage)
    }

    // ── Stone ─────────────────────────────────────────────────

    @Test
    fun `stone never appears in merge groups`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.stone(1, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.normal(4, 4, HexCoord(1, -1)))

        val groups = mergeEngine.findMergeableGroups(board)
        assertEquals(1, groups.size)
        assertFalse(groups[0].any { it.trait == TileTrait.STONE })
    }

    @Test
    fun `bomb clears stone`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.bomb(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.stone(4, HexCoord(-1, 0)))

        val result = chainEngine.resolveChains(board, testRandom())
        assertTrue(result.finalBoard.tileAt(HexCoord(-1, 0)) == null)
    }

    @Test
    fun `removeTile booster removes stone`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.stone(1, HexCoord(0, 0)))
        val state = com.mergeseven.game.game.model.GameState.initial(
            board = board,
            trayPieces = emptyList(),
            level = 1,
            targetValue = 64
        )

        val next = gameEngine.removeTile(state, HexCoord(0, 0))
        assertTrue(next.board.tileAt(HexCoord(0, 0)) == null)
    }

    // ── Multiplier ────────────────────────────────────────────

    @Test
    fun `multiplier times two boosts merge score`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.multiplier(1, 4, HexCoord(0, 0), 2))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(8L * 3L * 2L, resolution.event.scoreEarned)
        assertEquals(TileTrait.NORMAL, resolution.event.resultTile.trait)
        assertEquals(1, resolution.event.resultTile.multiplierFactor)
    }

    @Test
    fun `two multipliers multiply together`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.multiplier(1, 4, HexCoord(0, 0), 2))
        board = board.withTile(Tile.multiplier(2, 4, HexCoord(1, 0), 3))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(8L * 3L * 6L, resolution.event.scoreEarned)
    }

    @Test
    fun `multiplier factor does not persist on result tile`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.multiplier(1, 4, HexCoord(0, 0), 3))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(TileTrait.NORMAL, resolution.event.resultTile.trait)
        assertEquals(1, resolution.event.resultTile.multiplierFactor)
    }

    // ── Interactions ──────────────────────────────────────────

    @Test
    fun `bomb destroys adjacent frozen instead of thawing it`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.bomb(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.frozen(4, 8, HexCoord(-1, 0), stage = 2))

        val result = chainEngine.resolveChains(board, testRandom())
        assertTrue(result.finalBoard.tileAt(HexCoord(-1, 0)) == null)
    }

    @Test
    fun `wildcard plus multiplier applies multiplier to group score`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.multiplier(1, 4, HexCoord(0, 0), 2))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.wildcard(3, 2, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val resolution = mergeEngine.resolveGroup(board, groups[0], testRandom())
        assertEquals(8, resolution.event.resultTile.value)
        assertEquals(8L * 3L * 2L, resolution.event.scoreEarned)
    }

    @Test
    fun `frozen adjacent to merge thaws without joining the group`() {
        var board = boardEngine.createBoard(radius = 2)
        board = board.withTile(Tile.normal(1, 4, HexCoord(0, 0)))
        board = board.withTile(Tile.normal(2, 4, HexCoord(1, 0)))
        board = board.withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        board = board.withTile(Tile.frozen(4, 4, HexCoord(-1, 0), stage = 2))

        val beforeGroups = mergeEngine.findMergeableGroups(board)
        assertFalse(beforeGroups[0].any { it.trait == TileTrait.FROZEN })

        val result = chainEngine.resolveChains(board, testRandom())
        assertTrue(result.events.any { it is GameEvent.TilesThawed })
        assertEquals(1, result.finalBoard.tileAt(HexCoord(-1, 0))?.freezeStage)
    }

    private fun testRandom() = GameRandom(RngState.fromSeed(1L))
}
