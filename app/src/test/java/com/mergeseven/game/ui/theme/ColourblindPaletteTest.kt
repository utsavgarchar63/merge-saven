package com.mergeseven.game.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.mergeseven.game.data.preferences.ColourblindMode

class ColourblindPaletteTest {

    @Test
    fun `deutan palette yields distinct colors for ladder`() {
        val colors = (1..8).map { ColourblindPalette.tileColor(ColourblindMode.DEUTERANOPIA, 1 shl it) }
        assertEquals(8, colors.toSet().size)
    }

    @Test
    fun `protan differs from default for red-green pair`() {
        assertNotEquals(
            GameColors.tileColor(2),
            ColourblindPalette.tileColor(ColourblindMode.PROTANOPIA, 2)
        )
        assertNotEquals(
            GameColors.tileColor(4),
            ColourblindPalette.tileColor(ColourblindMode.PROTANOPIA, 4)
        )
    }

    @Test
    fun `tritan palette cycles stably`() {
        val a = ColourblindPalette.tileColor(ColourblindMode.TRITANOPIA, 2)
        val b = ColourblindPalette.tileColor(ColourblindMode.TRITANOPIA, 2)
        assertEquals(a, b)
        assertTrue(ColourblindPalette.valueIndex(8) > ColourblindPalette.valueIndex(2))
    }

    @Test
    fun `off mode delegates to GameColors`() {
        assertEquals(GameColors.tileColor(16), ColourblindPalette.tileColor(ColourblindMode.OFF, 16))
    }
}
