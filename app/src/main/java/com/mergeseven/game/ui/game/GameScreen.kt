package com.mergeseven.game.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.ui.theme.GameColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clean & Focused Hex Merge Game Screen with Animated Level Completion Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var trayBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragGlobalPosition by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .onGloballyPositioned { coords ->
                rootWindowOffset = coords.positionInWindow()
            }
            .statusBarsPadding()
            .pointerInput(uiState.trayPieces, rootWindowOffset, trayBounds) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val globalTouchPos = down.position + rootWindowOffset
                        val currentTrayBounds = trayBounds

                        // Check if touch is inside the bottom tray container
                        val slotIdx = if (currentTrayBounds != null && currentTrayBounds.contains(globalTouchPos)) {
                            val relativeX = (globalTouchPos.x - currentTrayBounds.left).coerceAtLeast(0f)
                            val colWidth = currentTrayBounds.width / 3f
                            val calculatedSlot = (relativeX / colWidth).toInt().coerceIn(0, 2)
                            if (uiState.trayPieces.getOrNull(calculatedSlot) != null) calculatedSlot else null
                        } else null

                        if (slotIdx == null) continue

                        // Start dragging instantly on touch down frame 1
                        draggingSlotIndex = slotIdx
                        dragGlobalPosition = globalTouchPos
                        viewModel.onSelectTraySlot(slotIdx)

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            val currentGlobalPos = change.position + rootWindowOffset

                            if (!change.pressed) {
                                // Finger released anywhere on screen!
                                val bounds = boardBounds
                                val activeSlot = draggingSlotIndex

                                if (bounds != null && activeSlot != null) {
                                    // Touch offset (-120px) so piece centers right above finger tip
                                    val targetX = currentGlobalPos.x - bounds.left
                                    val targetY = currentGlobalPos.y - bounds.top - 120f

                                    val hexSize = HexGeometry.calculateHexSize(
                                        uiState.boardRadius,
                                        bounds.width,
                                        bounds.height,
                                        8f
                                    )
                                    val centerX = bounds.width / 2f
                                    val centerY = bounds.height / 2f

                                    val targetCell = HexGeometry.nearestCell(
                                        targetX, targetY, hexSize, centerX, centerY, uiState.boardCells, hexSize * 2.2f
                                    )
                                    if (targetCell != null) {
                                        viewModel.onDropOnCell(targetCell, activeSlot)
                                    } else {
                                        viewModel.onCellHover(null, activeSlot)
                                    }
                                } else {
                                    if (activeSlot != null) viewModel.onCellHover(null, activeSlot)
                                }

                                draggingSlotIndex = null
                                dragGlobalPosition = null
                                break
                            }

                            change.consume()
                            dragGlobalPosition = currentGlobalPos

                            // Update board cell hover preview in real-time
                            val bounds = boardBounds
                            val activeSlot = draggingSlotIndex
                            if (bounds != null && activeSlot != null) {
                                val targetX = currentGlobalPos.x - bounds.left
                                val targetY = currentGlobalPos.y - bounds.top - 120f

                                val hexSize = HexGeometry.calculateHexSize(
                                    uiState.boardRadius,
                                    bounds.width,
                                    bounds.height,
                                    8f
                                )
                                val centerX = bounds.width / 2f
                                val centerY = bounds.height / 2f

                                val hoveredCell = HexGeometry.nearestCell(
                                    targetX, targetY, hexSize, centerX, centerY, uiState.boardCells, hexSize * 2.2f
                                )
                                viewModel.onCellHover(hoveredCell, activeSlot)
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ─── Top Bar ─────────────────────────────────
            GameTopBar(
                level = uiState.level,
                score = uiState.score,
                coins = uiState.coins,
                targetValue = uiState.targetValue,
                onPauseClick = { viewModel.onPause() }
            )

            // ─── Board Area ──────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        boardBounds = coordinates.boundsInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                HexBoardCanvas(
                    playableCells = uiState.boardCells,
                    tiles = uiState.tiles,
                    boardRadius = uiState.boardRadius,
                    hoveredCells = uiState.hoveredCells
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Bottom Area (3 Option Tray) ────────────
            ThreeOptionBottomTray(
                trayPieces = uiState.trayPieces,
                selectedSlotIndex = uiState.selectedSlotIndex,
                draggingSlotIndex = draggingSlotIndex,
                onTrayPositioned = { bounds ->
                    trayBounds = bounds
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // ─── Floating Drag Preview Overlay ──────────────
        val currentDragIdx = draggingSlotIndex
        val currentDragPos = dragGlobalPosition
        if (currentDragIdx != null && currentDragPos != null) {
            val piece = uiState.trayPieces.getOrNull(currentDragIdx)
            if (piece != null) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val floatX = currentDragPos.x
                    val floatY = currentDragPos.y - 120f
                    val previewHexSize = 36f

                    for (cell in piece.rotatedCells()) {
                        val (px, py) = HexGeometry.hexToPixel(cell.offset, previewHexSize, floatX, floatY)
                        drawHexTile(px, py, previewHexSize, GameColors.tileColor(cell.value), cell.value)
                    }
                }
            }
        }

        // ─── Level Complete Animated Overlay ────────────
        if (uiState.isLevelComplete) {
            LevelCompleteDialog(
                level = uiState.level,
                score = uiState.score,
                starsEarned = uiState.starsEarned,
                onNextLevel = { viewModel.onNextLevel() },
                onNavigateHome = onNavigateHome,
                onReplay = { viewModel.startNewGame() }
            )
        }

        // ─── Game Over Overlay ──────────────────────────
        if (uiState.isGameOver && !uiState.isLevelComplete) {
            GameOverDialog(
                score = uiState.score,
                onRestart = { viewModel.startNewGame() }
            )
        }
    }
}

@Composable
private fun LevelCompleteDialog(
    level: Int,
    score: Long,
    starsEarned: Int,
    onNextLevel: () -> Unit,
    onNavigateHome: () -> Unit,
    onReplay: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                border = androidx.compose.foundation.BorderStroke(3.dp, GameColors.CoinGold)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Badge
                    Surface(
                        shape = CircleShape,
                        color = GameColors.CoinGold,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "LEVEL $level COMPLETE!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = GameColors.CoinGold
                    )

                    // Animated Star Rating
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val isStarred = i <= starsEarned
                            val starScale by animateFloatAsState(
                                targetValue = if (isStarred && visible) 1f else 0.2f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                                label = "starScale$i"
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(36.dp * starScale)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GameColors.WoodDark.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Score Achieved",
                                style = MaterialTheme.typography.labelMedium,
                                color = GameColors.TextWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$score PTS",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = GameColors.TextWhite
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "🪙 +50 Coins Earned",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GameColors.CoinGold
                            )
                        }
                    }

                    // Action Buttons
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEXT LEVEL",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = onNavigateHome,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, GameColors.WoodLight),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Text(
                                text = "LEVEL MAP",
                                color = GameColors.TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onReplay,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, GameColors.WoodLight),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Text(
                                text = "REPLAY",
                                color = GameColors.TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(
    level: Int,
    score: Long,
    coins: Int,
    targetValue: Int,
    onPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPauseClick) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = GameColors.TextWhite
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level $level • Target: $targetValue",
                style = MaterialTheme.typography.titleSmall,
                color = GameColors.TextWhite.copy(alpha = 0.8f)
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = GameColors.CoinGold
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameColors.WoodLight.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🪙 $coins",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.CoinGold
                )
            }
        }
    }
}

@Composable
private fun HexBoardCanvas(
    playableCells: Set<HexCoord>,
    tiles: Map<HexCoord, Pair<Int, Color>>,
    boardRadius: Int,
    hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList()
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

        // Draw empty playable cells
        for (cell in playableCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            drawHexCell(px, py, hexSize, GameColors.BoardCellEmpty)
        }

        // Draw active tiles
        for ((coord, tileInfo) in tiles) {
            val (value, color) = tileInfo
            val (px, py) = HexGeometry.hexToPixel(coord, hexSize, centerX, centerY)
            drawHexTile(px, py, hexSize, color, value)
        }

        // Draw hover highlights for placement feedback
        for ((cell, isValid) in hoveredCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            val hoverColor = if (isValid) GameColors.BoardCellHighlight else GameColors.BoardCellInvalid
            drawHexCell(px, py, hexSize, hoverColor)
        }
    }
}

@Composable
private fun ThreeOptionBottomTray(
    trayPieces: List<TilePiece?>,
    selectedSlotIndex: Int,
    draggingSlotIndex: Int?,
    onTrayPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.onGloballyPositioned { coords ->
            onTrayPositioned(coords.boundsInRoot())
        },
        shape = RoundedCornerShape(20.dp),
        color = GameColors.WoodMid.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(2.dp, GameColors.WoodLight)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DRAG ANY PIECE TO BOARD",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until 3) {
                    val piece = trayPieces.getOrNull(index)
                    val isSelected = index == selectedSlotIndex
                    val isBeingDragged = index == draggingSlotIndex

                    PieceTrayOptionCard(
                        slotIndex = index,
                        piece = piece,
                        isSelected = isSelected,
                        isBeingDragged = isBeingDragged,
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PieceTrayOptionCard(
    slotIndex: Int,
    piece: TilePiece?,
    isSelected: Boolean,
    isBeingDragged: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) GameColors.CoinGold else GameColors.WoodLight.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val bgColor = if (isSelected) GameColors.WoodLight.copy(alpha = 0.35f) else GameColors.WoodDark.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (piece != null && !isBeingDragged) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val hexSize = minOf(size.width, size.height) / 4.2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                for (cell in piece.rotatedCells()) {
                    val (px, py) = HexGeometry.hexToPixel(cell.offset, hexSize, centerX, centerY)
                    drawHexTile(px, py, hexSize, GameColors.tileColor(cell.value), cell.value)
                }
            }

            // Option slot badge number (1, 2, 3)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp),
                shape = CircleShape,
                color = if (isSelected) GameColors.CoinGold else GameColors.WoodLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${slotIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        fontSize = 11.sp
                    )
                }
            }
        } else if (piece == null || isBeingDragged) {
            Text(
                text = if (isBeingDragged) "DRAGGING" else "EMPTY",
                style = MaterialTheme.typography.labelSmall,
                color = GameColors.TextWhite.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun GameOverDialog(
    score: Long,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )

                Text(
                    text = "Final Score: $score",
                    style = MaterialTheme.typography.titleLarge,
                    color = GameColors.CoinGold
                )

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                ) {
                    Text(
                        text = "PLAY AGAIN",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Draw empty hex cell outline.
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
 * Draw filled hex tile with number value.
 */
private fun DrawScope.drawHexTile(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color,
    value: Int
) {
    val tileSize = size * 0.9f

    val shadowPath = hexPath(centerX + 2f, centerY + 3f, tileSize)
    drawPath(shadowPath, Color.Black.copy(alpha = 0.3f), style = Fill)

    val mainPath = hexPath(centerX, centerY, tileSize)
    drawPath(mainPath, color, style = Fill)
    drawPath(mainPath, color.copy(alpha = 0.7f), style = Stroke(width = 2f))

    val highlightPath = hexPath(centerX, centerY - 1f, tileSize * 0.85f)
    drawPath(highlightPath, Color.White.copy(alpha = 0.15f), style = Fill)

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
