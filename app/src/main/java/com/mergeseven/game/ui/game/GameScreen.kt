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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
                boardRadius = uiState.boardRadius,
                showDebugGrid = uiState.showDebugGrid,
                debugSelectedCell = uiState.debugSelectedCell,
                hoveredCells = uiState.hoveredCells,
                onCellTapped = { viewModel.onCellTapped(it) },
                onCellHover = { viewModel.onCellHover(it) },
                onDragEnd = { viewModel.onDragEnd() }
            )
        }

        // ─── Bottom Area (Piece Queue + Boosters) ────
        GameBottomBar(
            currentPiece = uiState.currentPiece,
            nextPieces = uiState.nextPieces,
            onRotate = { viewModel.onRotatePiece() },
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
    boardRadius: Int,
    showDebugGrid: Boolean = false,
    debugSelectedCell: HexCoord? = null,
    hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList(),
    onCellTapped: (HexCoord) -> Unit = {},
    onCellHover: (HexCoord?) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .pointerInput(boardRadius, playableCells, "tap") {
                detectTapGestures { offset ->
                    val centerX = size.width.toFloat() / 2f
                    val centerY = size.height.toFloat() / 2f
                    val hexSize = HexGeometry.calculateHexSize(boardRadius, size.width.toFloat(), size.height.toFloat(), 8f)
                    val tappedCell = HexGeometry.nearestCell(offset.x, offset.y, hexSize, centerX, centerY, playableCells)
                    if (tappedCell != null) onCellTapped(tappedCell)
                }
            }
            .pointerInput(boardRadius, playableCells, "drag") {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width.toFloat() / 2f
                        val centerY = size.height.toFloat() / 2f
                        val hexSize = HexGeometry.calculateHexSize(boardRadius, size.width.toFloat(), size.height.toFloat(), 8f)
                        val hoveredCell = HexGeometry.nearestCell(offset.x, offset.y, hexSize, centerX, centerY, playableCells)
                        onCellHover(hoveredCell)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, _ ->
                        change.consume()
                        val offset = change.position
                        val centerX = size.width.toFloat() / 2f
                        val centerY = size.height.toFloat() / 2f
                        val hexSize = HexGeometry.calculateHexSize(boardRadius, size.width.toFloat(), size.height.toFloat(), 8f)
                        val hoveredCell = HexGeometry.nearestCell(offset.x, offset.y, hexSize, centerX, centerY, playableCells)
                        onCellHover(hoveredCell)
                    }
                )
            }
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
            val isSelected = cell == debugSelectedCell
            val cellColor = if (isSelected) GameColors.BoardCellHighlight else GameColors.BoardCellEmpty
            drawHexCell(px, py, hexSize, cellColor)
            
            // Debug grid coordinates
            if (showDebugGrid && !tiles.containsKey(cell)) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        this.textSize = hexSize * 0.3f
                        this.textAlign = android.graphics.Paint.Align.CENTER
                        this.alpha = 150
                    }
                    drawText("${cell.q},${cell.r}", px, py + hexSize * 0.1f, paint)
                }
            }
        }

        // Draw tiles
        for ((coord, tileInfo) in tiles) {
            val (value, color) = tileInfo
            val (px, py) = HexGeometry.hexToPixel(coord, hexSize, centerX, centerY)
            drawHexTile(px, py, hexSize, color, value)
            
            if (showDebugGrid) {
                val isSelected = coord == debugSelectedCell
                if (isSelected) {
                    drawHexCell(px, py, hexSize, GameColors.BoardCellHighlight)
                }
            }
        }

        // Draw hover highlights
        for ((cell, isValid) in hoveredCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            val hoverColor = if (isValid) GameColors.BoardCellHighlight else GameColors.BoardCellInvalid
            drawHexCell(px, py, hexSize, hoverColor)
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
private fun GameBottomBar(
    currentPiece: com.mergeseven.game.game.model.TilePiece?,
    nextPieces: List<com.mergeseven.game.game.model.TilePiece>,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        // Piece queue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PieceTray(
                piece = currentPiece,
                scale = 1f,
                onClick = onRotate,
                modifier = Modifier.size(80.dp)
            )
            val firstNext = nextPieces.firstOrNull()
            PieceTray(
                piece = firstNext,
                scale = 0.7f,
                modifier = Modifier.size(60.dp)
            )
            val secondNext = nextPieces.drop(1).firstOrNull()
            PieceTray(
                piece = secondNext,
                scale = 0.5f,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun PieceTray(
    piece: com.mergeseven.game.game.model.TilePiece?,
    scale: Float = 1f,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Surface(
        modifier = modifier.then(clickModifier),
        shape = MaterialTheme.shapes.medium,
        color = GameColors.WoodMid.copy(alpha = 0.5f),
        border = if (scale == 1f) androidx.compose.foundation.BorderStroke(2.dp, GameColors.WoodLight) else null
    ) {
        if (piece != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hexSize = size.width / 3f * scale
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                
                // Draw piece cells relative to center
                for (cell in piece.rotatedCells()) {
                    val (px, py) = com.mergeseven.game.game.engine.HexGeometry.hexToPixel(
                        cell.offset, hexSize, centerX, centerY
                    )
                    drawHexTile(px, py, hexSize, GameColors.tileColor(cell.value), cell.value)
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
