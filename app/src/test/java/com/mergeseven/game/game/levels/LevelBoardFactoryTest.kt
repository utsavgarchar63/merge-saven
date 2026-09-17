package com.mergeseven.game.game.levels

import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.GameRandom
import com.mergeseven.game.game.model.CellModifierType
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.TileTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDefinitionLoaderTest {

    private val loaderJson = LevelDefinitionLoader.json

    @Test
    fun `parses radius_minus_holes template`() {
        val definition = loaderJson.decodeFromString(
            LevelDefinition.serializer(),
            """
            {
              "level": 2,
              "target": 32,
              "board": {
                "mode": "radius_minus_holes",
                "radius": 3,
                "holes": [{"q": 0, "r": 0}]
              },
              "startingTiles": [
                {"q": 1, "r": 0, "value": 4, "trait": "STONE"}
              ],
              "modifiers": [
                {"q": 2, "r": 0, "type": "SCORE_PAD", "scoreBonus": 1.5}
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, definition.level)
        assertEquals(BoardTemplateMode.RADIUS_MINUS_HOLES, definition.board.mode)
        assertEquals(TileTrait.STONE, definition.startingTiles.single().trait)
        assertEquals(CellModifierType.SCORE_PAD, definition.modifiers.single().type)
    }

    @Test
    fun `parses explicit cell list`() {
        val definition = loaderJson.decodeFromString(
            LevelDefinition.serializer(),
            """
            {
              "level": 3,
              "board": {
                "mode": "explicit",
                "cells": [{"q": 0, "r": 0}, {"q": 1, "r": 0}]
              }
            }
            """.trimIndent()
        )

        assertEquals(BoardTemplateMode.EXPLICIT, definition.board.mode)
        assertEquals(2, definition.board.cells.size)
    }
}

class LevelBoardFactoryTest {

    private val boardEngine = BoardEngine()

    @Test
    fun `af1 off always returns default radial board`() {
        val factory = LevelBoardFactory(
            boardEngine = boardEngine,
            loader = unusedLoader(),
            featureFlags = InMemoryFeatureFlags(isDebug = true)
        )
        // AF1 defaults off
        val prepared = factory.prepare(2, GameRandom(RngState.fromSeed(1L)))
        assertEquals("default", prepared.boardProfile)
        assertEquals(boardEngine.cellCount(4), prepared.board.totalPlayable)
    }

    @Test
    fun `buildFromDefinition applies holes prefill and modifiers`() {
        val factory = LevelBoardFactory(
            boardEngine = boardEngine,
            loader = unusedLoader(),
            featureFlags = InMemoryFeatureFlags(isDebug = true, initial = mapOf(
                com.mergeseven.game.core.flags.Feature.AF1 to true
            ))
        )
        val definition = LevelDefinitionLoader.json.decodeFromString(
            LevelDefinition.serializer(),
            """
            {
              "level": 2,
              "target": 32,
              "board": {
                "mode": "radius_minus_holes",
                "radius": 2,
                "holes": [{"q": 0, "r": 0}]
              },
              "startingTiles": [
                {"q": 1, "r": 0, "value": 4, "trait": "FROZEN", "freezeStage": 2}
              ],
              "modifiers": [
                {"q": -1, "r": 0, "type": "LOCKED"}
              ]
            }
            """.trimIndent()
        )

        val prepared = factory.buildFromDefinition(definition, GameRandom(RngState.fromSeed(9L)))
        assertNotNull(prepared)
        assertFalse(prepared!!.board.isPlayable(HexCoord(0, 0)))
        assertEquals(TileTrait.FROZEN, prepared.board.tileAt(HexCoord(1, 0))?.trait)
        assertEquals(2, prepared.board.tileAt(HexCoord(1, 0))?.freezeStage)
        assertTrue(prepared.board.isLocked(HexCoord(-1, 0)))
        assertEquals(32, prepared.targetValue)
        assertEquals(1, prepared.objectives.size)
    }

    @Test
    fun `buildFromDefinition parses multi objectives and star moves`() {
        val factory = LevelBoardFactory(
            boardEngine = boardEngine,
            loader = unusedLoader(),
            featureFlags = InMemoryFeatureFlags(
                isDebug = true,
                initial = mapOf(com.mergeseven.game.core.flags.Feature.AF1 to true)
            )
        )
        val definition = LevelDefinitionLoader.json.decodeFromString(
            LevelDefinition.serializer(),
            """
            {
              "level": 4,
              "target": 32,
              "board": {
                "mode": "radius_minus_holes",
                "radius": 2,
                "holes": []
              },
              "objectives": [
                { "type": "score_in_moves", "id": "score2k", "score": 2000, "moves": 30 },
                { "type": "survive_moves", "id": "survive15", "moves": 15 }
              ],
              "starMoves": { "three": 18, "two": 28 }
            }
            """.trimIndent()
        )

        val prepared = factory.buildFromDefinition(definition, GameRandom(RngState.fromSeed(1L)))
        assertNotNull(prepared)
        assertEquals(18, prepared!!.threeStarMoveCap)
        assertEquals(28, prepared.twoStarMoveCap)
        assertEquals(2, prepared.objectives.size)
        assertTrue(
            prepared.objectives.any {
                it is com.mergeseven.game.game.objectives.LevelObjective.ScoreInMoves
            }
        )
    }

    @Test
    fun `invalid starting tile off the board returns null`() {
        val factory = LevelBoardFactory(
            boardEngine = boardEngine,
            loader = unusedLoader(),
            featureFlags = InMemoryFeatureFlags(isDebug = true)
        )
        val definition = LevelDefinition(
            level = 9,
            board = BoardTemplateDto(
                mode = BoardTemplateMode.EXPLICIT,
                cells = listOf(AxialDto(0, 0))
            ),
            startingTiles = listOf(
                StartingTileDto(q = 5, r = 5, value = 4)
            )
        )

        assertNull(factory.buildFromDefinition(definition, GameRandom(RngState.fromSeed(1L))))
    }

    private fun unusedLoader(): LevelDefinitionLoader {
        // Never called when using buildFromDefinition; Context is unused.
        return LevelDefinitionLoader.assetsOnly(
            context = com.mergeseven.game.testing.fakeContext()
        )
    }
}
