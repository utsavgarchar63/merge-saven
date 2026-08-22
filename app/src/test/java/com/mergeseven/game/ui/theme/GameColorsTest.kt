package com.mergeseven.game.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class GameColorsTest {

    @Test
    fun `test explicit tile colors up to 2048`() {
        assertEquals(GameColors.TileBlue, GameColors.tileColor(2))
        assertEquals(GameColors.TileGreen, GameColors.tileColor(4))
        assertEquals(GameColors.TileRed, GameColors.tileColor(8))
        assertEquals(GameColors.TilePurple, GameColors.tileColor(16))
        assertEquals(GameColors.TilePink, GameColors.tileColor(32))
        assertEquals(GameColors.TileGold, GameColors.tileColor(64))
        assertEquals(GameColors.TileTeal, GameColors.tileColor(128))
        assertEquals(GameColors.TileOrange, GameColors.tileColor(256))
        assertEquals(GameColors.TileIndigo, GameColors.tileColor(512))
        assertEquals(GameColors.TileLime, GameColors.tileColor(1024))
        assertEquals(GameColors.TileCyan, GameColors.tileColor(2048))
    }

    @Test
    fun `test fallback infinite color cycle beyond 2048`() {
        // Beyond 2048, the algorithm uses a subset of 8 colors:
        // Blue, Green, Red, Purple, Pink, Gold, Teal, Orange
        // 4096 = 2^12. Trailing zeros = 12. Index = (12 - 1) % 8 = 11 % 8 = 3.
        // Index 3 in the fallback array is TilePurple.
        assertEquals(GameColors.TilePurple, GameColors.tileColor(4096))
        
        // 8192 = 2^13. Trailing zeros = 13. Index = (13 - 1) % 8 = 12 % 8 = 4 -> TilePink
        assertEquals(GameColors.TilePink, GameColors.tileColor(8192))
        
        // 16384 = 2^14 -> TileGold
        assertEquals(GameColors.TileGold, GameColors.tileColor(16384))
        
        // Massive number: 2^24 = 16,777,216.
        // Trailing zeros = 24. Index = (24 - 1) % 8 = 23 % 8 = 7 -> TileOrange
        assertEquals(GameColors.TileOrange, GameColors.tileColor(16777216))
    }
}
