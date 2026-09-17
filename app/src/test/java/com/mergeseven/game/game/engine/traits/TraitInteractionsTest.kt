package com.mergeseven.game.game.engine.traits

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TileTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitInteractionsTest {

    @Test
    fun `stone and frozen cannot participate`() {
        assertFalse(TraitInteractions.canParticipateInMerge(Tile.stone(1, HexCoord(0, 0))))
        assertFalse(TraitInteractions.canParticipateInMerge(Tile.frozen(2, 4, HexCoord(0, 0), stage = 2)))
        assertFalse(TraitInteractions.canParticipateInMerge(Tile.frozen(3, 4, HexCoord(0, 0), stage = 1)))
    }

    @Test
    fun `normal bomb wildcard multiplier can participate`() {
        assertTrue(TraitInteractions.canParticipateInMerge(Tile.normal(1, 4, HexCoord(0, 0))))
        assertTrue(TraitInteractions.canParticipateInMerge(Tile.bomb(2, 4, HexCoord(0, 0))))
        assertTrue(TraitInteractions.canParticipateInMerge(Tile.wildcard(3, 2, HexCoord(0, 0))))
        assertTrue(TraitInteractions.canParticipateInMerge(Tile.multiplier(4, 4, HexCoord(0, 0), 2)))
    }

    @Test
    fun `wildcard connects to any established value`() {
        assertTrue(TraitInteractions.canConnect(Tile.wildcard(1, 2, HexCoord(0, 0)), groupValue = 8))
        assertTrue(TraitInteractions.canConnect(Tile.normal(2, 8, HexCoord(0, 0)), groupValue = 8))
        assertFalse(TraitInteractions.canConnect(Tile.normal(3, 4, HexCoord(0, 0)), groupValue = 8))
    }

    @Test
    fun `thaw reduces stage then clears trait`() {
        val solid = Tile.frozen(1, 4, HexCoord(0, 0), stage = 2)
        val cracked = TraitInteractions.applyThaw(solid)
        assertEquals(TileTrait.FROZEN, cracked.trait)
        assertEquals(1, cracked.freezeStage)

        val thawed = TraitInteractions.applyThaw(cracked)
        assertEquals(TileTrait.NORMAL, thawed.trait)
        assertEquals(0, thawed.freezeStage)
    }

    @Test
    fun `score multiplies factors and bomb penalty`() {
        val group = listOf(
            Tile.multiplier(1, 4, HexCoord(0, 0), 2),
            Tile.multiplier(2, 4, HexCoord(1, 0), 3),
            Tile.bomb(3, 4, HexCoord(0, 1))
        )
        assertEquals(2f * 3f * Constants.BOMB_SCORE_FACTOR, TraitInteractions.scoreTraitMultiplier(group), 0.001f)
    }

    @Test
    fun `merged result doubles established non-wildcard value`() {
        val group = listOf(
            Tile.wildcard(1, 2, HexCoord(0, 0)),
            Tile.normal(2, 8, HexCoord(1, 0)),
            Tile.wildcard(3, 2, HexCoord(0, 1))
        )
        assertEquals(8, TraitInteractions.establishedValue(group))
        assertEquals(16, TraitInteractions.mergedResultValue(group))
    }
}
