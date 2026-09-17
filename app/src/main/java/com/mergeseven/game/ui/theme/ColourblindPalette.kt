package com.mergeseven.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.data.preferences.ColourblindMode

/**
 * AF11-01 colourblind-safe remaps of the power-of-two tile ladder.
 * Avoids red–green pairs for deutan/protan; avoids blue–yellow pairs for tritan.
 */
object ColourblindPalette {

    // Deuteranopia / protanopia: blue–orange–yellow–brown–gray families
    private val DeutanProtan = listOf(
        Color(0xFF0072B2), // blue
        Color(0xFFE69F00), // orange
        Color(0xFF56B4E9), // sky
        Color(0xFFCC79A7), // reddish purple (safe vs green)
        Color(0xFFF0E442), // yellow
        Color(0xFFD55E00), // vermillion/brown-orange
        Color(0xFF999999), // gray
        Color(0xFF009E73), // bluish green
        Color(0xFF5C4033), // brown
        Color(0xFFBDB76B), // khaki
        Color(0xFF4A6FA5)  // steel blue
    )

    // Tritanopia: red–green–magenta–gray (avoid blue/yellow confusion)
    private val Tritan = listOf(
        Color(0xFFD55E00), // orange-red
        Color(0xFF009E73), // green
        Color(0xFFCC79A7), // magenta
        Color(0xFF666666), // dark gray
        Color(0xFFE69F00), // amber
        Color(0xFF56B4E9), // cyan-ish (kept distinct from yellow)
        Color(0xFF8B0000), // dark red
        Color(0xFF228B22), // forest
        Color(0xFFDB7093), // pale violet red
        Color(0xFF2F4F4F), // dark slate
        Color(0xFFFF7F50)  // coral
    )

    fun tileColor(mode: ColourblindMode, value: Int): Color {
        if (mode == ColourblindMode.OFF) return GameColors.tileColor(value)
        val palette = when (mode) {
            ColourblindMode.TRITANOPIA -> Tritan
            ColourblindMode.DEUTERANOPIA,
            ColourblindMode.PROTANOPIA -> DeutanProtan
            ColourblindMode.OFF -> return GameColors.tileColor(value)
        }
        val index = valueIndex(value) % palette.size
        return palette[index]
    }

    /** Stable index for shape cues and palette cycling (2→0, 4→1, …). */
    fun valueIndex(value: Int): Int {
        if (value <= 0) return 0
        val zeros = Integer.numberOfTrailingZeros(value)
        return (zeros - 1).coerceAtLeast(0)
    }
}
