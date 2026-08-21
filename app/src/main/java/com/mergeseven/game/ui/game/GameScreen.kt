package com.mergeseven.game.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.ui.theme.GameColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main game screen.
 * See Master Plan Section 28.
 *
 * Top: Pause, Level/Goal, Coins
 * Center: Progress indicator, Hex board
 * Bottom: Boosters, Piece queue
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        // ─── Top Bar ─────────────────────────────────
        GameTopBar(
            level = uiState.level,
            score = uiState.score,
            coins = uiState.coins,
            onPauseClick = { viewModel.onPause() }
        )

        // ─── Board Area ──────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HexBoardCanvas(
                playableCells = uiState.boardCells,
                tiles = uiState.tiles,
                boardRadius = uiState.boardRadius
            )
        }

        // ─── Bottom Area (Piece Queue + Boosters) ────
        GameBottomBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun GameTopBar(
    level: Int,
    score: Long,
    coins: Int,
    onPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause button
        IconButton(onClick = onPauseClick) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = GameColors.TextWhite
            )
        }

        // Level / Score
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level $level",
                style = MaterialTheme.typography.titleSmall,
                color = GameColors.TextWhite.copy(alpha = 0.7f)
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = GameColors.TextWhite
            )
        }

        // Coins
        Text(
            text = "🪙 $coins",
            style = MaterialTheme.typography.titleMedium,
            color = GameColors.CoinGold
        )
    }
}

/**
 * Canvas-based hex board renderer.
 * See Master Plan Section 9.3 (Board renderer).
 */
@Composable
private fun HexBoardCanvas(
    playableCells: Set<HexCoord>,
    tiles: Map<HexCoord, Pair<Int, Color>>,
    boardRadius: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val hexSize = HexGeometry.calculateHexSize(
            boardRadius = boardRadius,
            availableWidth = size.width,
            availableHeight = size.height,
            padding = 8f
        )

        // Draw empty cells
        for (cell in playableCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            drawHexCell(px, py, hexSize, GameColors.BoardCellEmpty)
        }

        // Draw tiles
        for ((coord, tileInfo) in tiles) {
            val (value, color) = tileInfo
            val (px, py) = HexGeometry.hexToPixel(coord, hexSize, centerX, centerY)
            drawHexTile(px, py, hexSize, color, value)
        }
    }
}

/**
 * Draw an empty hex cell outline.
 */
private fun DrawScope.drawHexCell(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val path = hexPath(centerX, centerY, size * 0.95f)
    drawPath(path, color, style = Fill)
    drawPath(path, color.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
}

/**
 * Draw a filled hex tile with number.
 * See Master Plan Section 35 (Tile treatment).
 */
private fun DrawScope.drawHexTile(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color,
    value: Int
) {
    val tileSize = size * 0.9f

    // Shadow
    val shadowPath = hexPath(centerX + 2f, centerY + 3f, tileSize)
    drawPath(shadowPath, Color.Black.copy(alpha = 0.3f), style = Fill)

    // Main fill
    val mainPath = hexPath(centerX, centerY, tileSize)
    drawPath(mainPath, color, style = Fill)

    // Border
    drawPath(mainPath, color.copy(alpha = 0.7f), style = Stroke(width = 2f))

    // Inner highlight (top-left area)
    val highlightPath = hexPath(centerX, centerY - 1f, tileSize * 0.85f)
    drawPath(highlightPath, Color.White.copy(alpha = 0.15f), style = Fill)

    // Number text
    drawContext.canvas.nativeCanvas.apply {
        val textSize = when {
            value >= 1000 -> tileSize * 0.35f
            value >= 100 -> tileSize * 0.45f
            else -> tileSize * 0.55f
        }
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            this.textSize = textSize
            this.textAlign = android.graphics.Paint.Align.CENTER
            this.isFakeBoldText = true
            this.isAntiAlias = true
        }
        val textY = centerY + textSize / 3f
        drawText("$value", centerX, textY, paint)
    }
}

/**
 * Create a hex Path for flat-top orientation.
 */
private fun hexPath(
    centerX: Float,
    centerY: Float,
    size: Float
): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angleDeg = 60f * i
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val x = centerX + size * cos(angleRad)
        val y = centerY + size * sin(angleRad)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
private fun GameBottomBar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Booster slots (placeholder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Swap", "Shuffle", "Remove", "Undo").forEach { label ->
                BoosterSlot(label = label)
            }
        }

        // Piece queue placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) { index ->
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = GameColors.WoodMid.copy(alpha = 0.5f),
                    border = if (index == 0) {
                        ButtonDefaults.outlinedButtonBorder(enabled = true)
                    } else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (index == 0) "▶" else "?",
                            color = GameColors.TextWhite.copy(
                                alpha = if (index == 0) 1f else 0.5f
                            ),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterSlot(label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.small,
            color = GameColors.WoodLight.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label.first().toString(),
                    color = GameColors.TextWhite.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GameColors.TextWhite.copy(alpha = 0.5f)
        )
    }
}
