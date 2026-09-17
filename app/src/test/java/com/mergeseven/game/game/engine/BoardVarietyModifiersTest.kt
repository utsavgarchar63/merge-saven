package com.mergeseven.game.game.engine

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.game.levels.LevelBoardFactory
import com.mergeseven.game.game.levels.LevelDefinition
import com.mergeseven.game.game.levels.LevelDefinitionLoader
import com.mergeseven.game.game.model.CellModifier
import com.mergeseven.game.game.model.CellModifierType
import com.mergeseven.game.game.model.GameEvent
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.testing.fakeContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BoardVarietyModifiersTest {

    private lateinit var boardEngine: BoardEngine
    private lateinit var mergeEngine: MergeEngine
    private lateinit var chainEngine: ChainReactionEngine
    private lateinit var placementEngine: PlacementEngine
    private lateinit var spawnEngine: SpawnEngine
    private lateinit var gameEngine: GameEngineImpl

    @Before
    fun setup() {
        boardEngine = BoardEngine()
        val scoreEngine = ScoreEngine()
        mergeEngine = MergeEngine(scoreEngine)
        chainEngine = ChainReactionEngine(mergeEngine)
        placementEngine = PlacementEngine()
        spawnEngine = SpawnEngine()
        gameEngine = GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = mergeEngine,
            placementEngine = placementEngine,
            spawnEngine = spawnEngine,
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placementEngine),
            chainReactionEngine = chainEngine
        )
    }

    @Test
    fun `explicit playable set creates non-radial board`() {
        val cells = setOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1))
        val board = boardEngine.createBoard(cells)
        assertEquals(3, board.totalPlayable)
        assertTrue(board.isPlayable(HexCoord(0, 0)))
        assertFalse(board.isPlayable(HexCoord(-1, 0)))
    }

    @Test
    fun `locked cell blocks placement until adjacent merge clears it`() {
        var board = boardEngine.createBoard(radius = 2)
        val locked = HexCoord(2, 0)
        board = board.withModifiers(
            mapOf(locked to CellModifier(CellModifierType.LOCKED))
        )
        board = board
            .withTile(Tile.normal(1, 4, HexCoord(0, 0)))
            .withTile(Tile.normal(2, 4, HexCoord(1, 0)))
            .withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val piece = singleCellPiece(4)
        assertFalse(placementEngine.canPlace(board, piece, locked))

        val result = chainEngine.resolveChains(board, GameRandom(RngState.fromSeed(1L)))
        assertFalse(result.finalBoard.isLocked(locked))
        assertTrue(placementEngine.canPlace(result.finalBoard, piece, locked))
    }

    @Test
    fun `score pad at destination multiplies merge score`() {
        var board = boardEngine.createBoard(radius = 2)
        val destination = HexCoord(0, 0)
        board = board.withModifiers(
            mapOf(destination to CellModifier(CellModifierType.SCORE_PAD, scoreBonus = 2f))
        )
        board = board
            .withTile(Tile.normal(1, 4, destination))
            .withTile(Tile.normal(2, 4, HexCoord(1, 0)))
            .withTile(Tile.normal(3, 4, HexCoord(0, 1)))

        val groups = mergeEngine.findMergeableGroups(board)
        val withPad = mergeEngine.resolveGroup(
            board,
            groups[0],
            GameRandom(RngState.fromSeed(1L)),
            preferredDestination = destination
        )

        var plain = boardEngine.createBoard(radius = 2)
        plain = plain
            .withTile(Tile.normal(1, 4, destination))
            .withTile(Tile.normal(2, 4, HexCoord(1, 0)))
            .withTile(Tile.normal(3, 4, HexCoord(0, 1)))
        val withoutPad = mergeEngine.resolveGroup(
            plain,
            mergeEngine.findMergeableGroups(plain)[0],
            GameRandom(RngState.fromSeed(1L)),
            preferredDestination = destination
        )

        assertEquals(withoutPad.event.scoreEarned * 2, withPad.event.scoreEarned)
    }

    @Test
    fun `spawn vent fills an empty vent after a move`() {
        var board = boardEngine.createBoard(radius = 2)
        val vent = HexCoord(-2, 0)
        board = board.withModifiers(mapOf(vent to CellModifier(CellModifierType.SPAWN_VENT)))

        val state = com.mergeseven.game.game.model.GameState.initial(
            board = board,
            trayPieces = listOf(singleCellPiece(2), singleCellPiece(2), singleCellPiece(2)),
            level = 1,
            targetValue = 64,
            rng = RngState.fromSeed(42L)
        )

        assertNull(state.board.tileAt(vent))
        val origin = HexCoord(0, 0)
        val result = gameEngine.placePiece(state, state.trayPieces[0]!!, origin, 0)
        assertTrue(result.events.any { it is GameEvent.VentSpawned })
        assertNotNull(result.state.board.tileAt(vent))
    }

    @Test
    fun `level factory with AF1 on uses JSON definition when present`() {
        val flags = InMemoryFeatureFlags(
            isDebug = true,
            initial = mapOf(Feature.AF1 to true)
        )
        // Use buildFromDefinition path via factory + sample parse (no assets in JVM unit test).
        val factory = LevelBoardFactory(
            boardEngine = boardEngine,
            loader = LevelDefinitionLoader.assetsOnly(fakeContext()),
            featureFlags = flags
        )
        val definition = LevelDefinitionLoader.json.decodeFromString(
            LevelDefinition.serializer(),
            """
            {
              "level": 2,
              "target": 32,
              "board": { "mode": "radius_minus_holes", "radius": 2, "holes": [{"q":0,"r":0}] },
              "startingTiles": [{"q":1,"r":0,"value":4,"trait":"STONE"}],
              "modifiers": [{"q":-1,"r":0,"type":"SPAWN_VENT"}]
            }
            """.trimIndent()
        )
        val prepared = factory.buildFromDefinition(definition, GameRandom(RngState.fromSeed(1L)))!!
        assertEquals(TileTrait.STONE, prepared.board.tileAt(HexCoord(1, 0))?.trait)
        assertEquals(CellModifierType.SPAWN_VENT, prepared.board.modifierAt(HexCoord(-1, 0))?.type)
    }

    private fun singleCellPiece(value: Int) = TilePiece(
        id = 100L + value,
        cells = listOf(PieceCell(HexCoord(0, 0), value))
    )
}
