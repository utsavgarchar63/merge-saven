package com.mergeseven.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.ui.game.TileMaterial
import com.mergeseven.game.ui.game.drawHexCell
import com.mergeseven.game.ui.game.drawHexHighlight
import com.mergeseven.game.ui.game.drawHexTile
import com.mergeseven.game.ui.game.rememberTileTypeface
import com.mergeseven.game.ui.theme.GameColors
import com.mergeseven.game.ui.theme.MergeSevenTheme
import com.mergeseven.game.ui.theme.TileThemes
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercise real Android drawing, including small tiles and fitted multi-digit labels. */
class HexTileRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun materialsTraitsAndLargeValuesRenderOnAndroid() {
        val themes = listOf("classic", "tile_marble", "tile_neon", "tile_seasonal", "tile_premium")
        val values = listOf(2, 4, 8, 16, 32, 1024, Int.MAX_VALUE)
        compose.setContent {
            MergeSevenTheme {
                val typeface = rememberTileTypeface()
                Column(Modifier.fillMaxSize().background(GameColors.WoodDark).padding(12.dp)) {
                    Text("Tile materials · 2× number scale", color = GameColors.TextWhite)
                    themes.forEach { id ->
                        val theme = TileThemes.of(id)
                        Text(theme.id, color = GameColors.CoinGold)
                        Canvas(Modifier.fillMaxWidth().weight(1f)) {
                            val width = size.width / values.size
                            values.forEachIndexed { index, value ->
                                drawHexTile(width * (index + 0.5f), size.height / 2f,
                                    minOf(width * 0.47f, size.height * 0.48f), theme.tileColor(value), value,
                                    fontScale = 2f, material = TileMaterial.of(theme.id), typeface = typeface)
                            }
                        }
                    }
                    Text("Special traits", color = GameColors.CoinGold)
                    Canvas(Modifier.fillMaxWidth().weight(1f)) {
                        val width = size.width / TileTrait.entries.size
                        TileTrait.entries.forEachIndexed { index, trait ->
                            drawHexTile(width * (index + 0.5f), size.height / 2f,
                                minOf(width * 0.45f, size.height * 0.45f), GameColors.TileBlue, 8,
                                trait = trait, freezeStage = 1, multiplierFactor = 3, typeface = typeface)
                        }
                    }
                    Text("Empty · selected · occupied · hint", color = GameColors.CoinGold)
                    Canvas(Modifier.fillMaxWidth().weight(1f)) {
                        val width = size.width / 4
                        repeat(4) { index ->
                            val x = width * (index + 0.5f); val y = size.height / 2f; val r = size.height * 0.4f
                            drawHexCell(x, y, r, GameColors.BoardCellEmpty)
                            if (index == 1) drawHexHighlight(x, y, r, GameColors.BoardCellHighlight)
                            if (index == 2) drawHexTile(x, y, r, GameColors.TileGreen, 4, typeface = typeface)
                            if (index == 3) drawHexHighlight(x, y, r, GameColors.BoardCellHint)
                        }
                    }
                }
            }
        }
        val image = compose.onRoot().captureToImage()
        val pixels = image.toPixelMap()
        var bright = 0; var dark = 0
        // Confirm the renderer produced both readable glyphs/highlights and recessed surfaces.
        for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) {
            val color = pixels[x, y]
            if (color.red + color.green + color.blue > 2f) bright++
            if (color.red + color.green + color.blue < 0.15f) dark++
        }
        assertTrue("No bright faces or text were rendered", bright > 100)
        assertTrue("No dark number glyphs or recesses were rendered", dark > 20)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "tile-materials-preview.png").outputStream().use {
            image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun invalidGeometryDoesNotPaintOrCrash() {
        compose.setContent {
            Canvas(Modifier.fillMaxSize().background(Color.Black)) {
                drawHexTile(Float.NaN, 40f, 20f, Color.Red, 2)
                drawHexTile(40f, Float.POSITIVE_INFINITY, 20f, Color.Red, 2)
                drawHexTile(40f, 40f, -1f, Color.Red, 2)
                drawHexCell(40f, 40f, 0f, Color.Red)
                drawHexHighlight(40f, 40f, Float.NaN, Color.Red)
            }
        }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        for (y in 0 until pixels.height step 10) for (x in 0 until pixels.width step 10) {
            assertTrue("Invalid geometry painted a pixel", pixels[x, y] == Color.Black)
        }
    }
}
