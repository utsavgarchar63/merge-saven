package com.mergeseven.game.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.content.res.Configuration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.core.Constants
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.model.CellModifier
import com.mergeseven.game.game.model.CellModifierType
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.game.objectives.ObjectiveProgress
import com.mergeseven.game.ui.components.CoinIcon
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.components.StarIcon
import com.mergeseven.game.ui.feel.ComboBanner
import com.mergeseven.game.ui.feel.DropTrailOverlay
import com.mergeseven.game.ui.feel.JuiceController
import com.mergeseven.game.ui.feel.JuiceUiState
import com.mergeseven.game.ui.feel.ParticleCanvas
import com.mergeseven.game.ui.feel.rememberShakeOffset
import com.mergeseven.game.ui.feel.shakeGraphics
import com.mergeseven.game.ui.theme.BoardThemes
import com.mergeseven.game.ui.theme.GameColors
import com.mergeseven.game.ui.theme.TileThemes
import com.mergeseven.game.ui.theme.withColourblindMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clean & Focused Hex Merge Game Screen with Animated Level Completion Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit = {},
    onNavigateShop: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val juiceState by viewModel.juiceUiState.collectAsStateWithLifecycle()
    val boardTheme = BoardThemes.of(uiState.boardThemeId)
    val tileTheme = TileThemes.of(uiState.tileThemeId).withColourblindMode(uiState.colourblindMode)
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val useSidePane = uiState.af11Enabled && (isLandscape || isTablet)

    // Flush the board to disk whenever the screen leaves the foreground; the debounced autosave
    // may still be pending, and the process can be killed at any time after this point.
    LifecycleStartEffect(viewModel) {
        onStopOrDispose { viewModel.onStopped() }
    }

    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var trayBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragGlobalPosition by remember { mutableStateOf<Offset?>(null) }
    var focusedCell by remember { mutableStateOf<HexCoord?>(null) }

    val announcementText = uiState.accessibilityAnnouncement?.let { ann ->
        when (ann.kind) {
            AccessibilityAnnounceKind.PLACED ->
                stringResource(R.string.game_announce_placed, ann.score)
            AccessibilityAnnounceKind.MERGED ->
                stringResource(R.string.game_announce_merge, ann.score)
            AccessibilityAnnounceKind.INVALID ->
                stringResource(R.string.game_announce_invalid)
            AccessibilityAnnounceKind.LEVEL_COMPLETE ->
                stringResource(R.string.game_announce_level_complete)
            AccessibilityAnnounceKind.GAME_OVER ->
                stringResource(R.string.game_announce_game_over, ann.score)
        }
    }

    LaunchedEffect(uiState.accessibilityAnnouncement?.nonce) {
        if (uiState.accessibilityAnnouncement != null) {
            kotlinx.coroutines.delay(1500)
            viewModel.dismissAccessibilityAnnouncement()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootWindowOffset = coords.positionInWindow()
            }
            .pointerInput(uiState.trayPieces, rootWindowOffset, trayBounds, layoutDirection) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val globalTouchPos = down.position + rootWindowOffset
                        val currentTrayBounds = trayBounds

                        // Check if touch is inside the bottom tray container
                        val slotIdx = if (currentTrayBounds != null && currentTrayBounds.contains(globalTouchPos)) {
                            val relativeX = (globalTouchPos.x - currentTrayBounds.left).coerceAtLeast(0f)
                            val colWidth = currentTrayBounds.width / 3f
                            val ltrSlot = (relativeX / colWidth).toInt().coerceIn(0, 2)
                            val calculatedSlot = if (layoutDirection == LayoutDirection.Rtl) {
                                2 - ltrSlot
                            } else {
                                ltrSlot
                            }
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
                                val boardPadding = if (uiState.largeTouchTargets) 4f else 8f

                                if (bounds != null && activeSlot != null) {
                                    // Touch offset (-120px) so piece centers right above finger tip
                                    val targetX = currentGlobalPos.x - bounds.left
                                    val targetY = currentGlobalPos.y - bounds.top - 120f

                                    val hexSize = HexGeometry.calculateHexSize(
                                        uiState.boardRadius,
                                        bounds.width,
                                        bounds.height,
                                        boardPadding
                                    )
                                    val centerX = bounds.width / 2f
                                    val centerY = bounds.height / 2f

                                    val targetCell = HexGeometry.nearestCell(
                                        targetX, targetY, hexSize, centerX, centerY, uiState.boardCells, hexSize * 2.2f
                                    )
                                    if (targetCell != null) {
                                        val boardLocal = Offset(targetX, targetY)
                                        viewModel.onDropOnCell(
                                            origin = targetCell,
                                            slotIndex = activeSlot,
                                            dropBoardLocal = boardLocal,
                                            boardWidth = bounds.width,
                                            boardHeight = bounds.height
                                        )
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
                            val boardPadding = if (uiState.largeTouchTargets) 4f else 8f
                            if (bounds != null && activeSlot != null) {
                                val targetX = currentGlobalPos.x - bounds.left
                                val targetY = currentGlobalPos.y - bounds.top - 120f

                                val hexSize = HexGeometry.calculateHexSize(
                                    uiState.boardRadius,
                                    bounds.width,
                                    bounds.height,
                                    boardPadding
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
        Image(
            painter = painterResource(boardTheme.backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (uiState.paletteKey == "zen") {
                        Color(0xFF1B3A2F).copy(alpha = 0.55f)
                    } else {
                        boardTheme.overlayColor
                    }
                )
                .statusBarsPadding()
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
                objectiveChips = if (uiState.showObjectives) uiState.objectiveChips else emptyList(),
                showTarget = uiState.showTarget,
                showTimer = uiState.showTimer,
                timeRemainingMs = uiState.timeRemainingMs,
                onPauseClick = { viewModel.onPause() }
            )

            if (announcementText != null) {
                Text(
                    text = announcementText,
                    modifier = Modifier
                        .semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = announcementText
                        }
                        .size(0.dp),
                    color = Color.Transparent
                )
            }

            if (useSidePane) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    GameBoardSection(
                        uiState = uiState,
                        boardTheme = boardTheme,
                        juiceState = juiceState,
                        juice = viewModel.juiceControllerPublic,
                        focusedCell = focusedCell,
                        onFocusedCellChange = { focusedCell = it },
                        onBoardBounds = { boardBounds = it },
                        onCellTapped = viewModel::onCellTapped,
                        onPlace = { cell -> viewModel.onDropOnCell(cell) },
                        onSelectTraySlot = viewModel::onSelectTraySlot,
                        onClearDropSnap = viewModel::clearDropSnap,
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    ) {
                        GameHintRow(uiState = uiState, viewModel = viewModel)
                        BoosterTray(
                            buttons = uiState.boosterButtons,
                            af3Enabled = uiState.af3Enabled,
                            largeTouchTargets = uiState.largeTouchTargets,
                            onBooster = { type -> dispatchBooster(viewModel, type) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ThreeOptionBottomTray(
                            trayPieces = uiState.trayPieces,
                            selectedSlotIndex = uiState.selectedSlotIndex,
                            draggingSlotIndex = draggingSlotIndex,
                            tileTheme = tileTheme,
                            colourblindMode = uiState.colourblindMode,
                            largeTouchTargets = uiState.largeTouchTargets,
                            onSelectTraySlot = viewModel::onSelectTraySlot,
                            onRotatePiece = viewModel::onRotateTraySlot,
                            onTrayPositioned = { bounds -> trayBounds = bounds },
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                // ─── Board Area ──────────────────────────────
                GameBoardSection(
                    uiState = uiState,
                    boardTheme = boardTheme,
                    juiceState = juiceState,
                    juice = viewModel.juiceControllerPublic,
                    focusedCell = focusedCell,
                    onFocusedCellChange = { focusedCell = it },
                    onBoardBounds = { boardBounds = it },
                    onCellTapped = viewModel::onCellTapped,
                    onPlace = { cell -> viewModel.onDropOnCell(cell) },
                    onSelectTraySlot = viewModel::onSelectTraySlot,
                    onClearDropSnap = viewModel::clearDropSnap,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                GameHintRow(uiState = uiState, viewModel = viewModel)

                BoosterTray(
                    buttons = uiState.boosterButtons,
                    af3Enabled = uiState.af3Enabled,
                    largeTouchTargets = uiState.largeTouchTargets,
                    onBooster = { type -> dispatchBooster(viewModel, type) },
                    modifier = Modifier.fillMaxWidth()
                )

                // ─── Bottom Area (3 Option Tray) ────────────
                ThreeOptionBottomTray(
                    trayPieces = uiState.trayPieces,
                    selectedSlotIndex = uiState.selectedSlotIndex,
                    draggingSlotIndex = draggingSlotIndex,
                    tileTheme = tileTheme,
                    colourblindMode = uiState.colourblindMode,
                    largeTouchTargets = uiState.largeTouchTargets,
                    onSelectTraySlot = viewModel::onSelectTraySlot,
                    onRotatePiece = viewModel::onRotateTraySlot,
                    onTrayPositioned = { bounds ->
                        trayBounds = bounds
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        uiState.achievementToast?.let { title ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.dismissAchievementToast() }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            ) {
                Text(stringResource(R.string.game_achievement, title))
            }
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
                        drawHexTile(
                            px,
                            py,
                            previewHexSize,
                            tileTheme.tileColor(cell.value),
                            cell.value,
                            showShapeCue = uiState.colourblindMode != ColourblindMode.OFF
                        )
                    }
                }
            }
        }

        // ─── Level Complete Animated Overlay ────────────
        if (uiState.isLevelComplete) {
            val context = LocalContext.current
            val activity = context as? android.app.Activity
            LevelCompleteDialog(
                level = uiState.level,
                score = uiState.score,
                starsEarned = uiState.starsEarned,
                af8Enabled = uiState.af8Enabled,
                submitStatus = uiState.scoreSubmitStatus,
                canDoubleCoins = uiState.canDoubleCoins,
                onShare = {
                    viewModel.createShareIntent()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share run"))
                    }
                },
                onDoubleCoins = { activity?.let { viewModel.onDoubleCoinsAd(it) } },
                onNextLevel = { viewModel.onNextLevel(keepBoard = true) },
                onNextLevelFresh = { viewModel.onNextLevel(keepBoard = false) },
                onNavigateHome = onNavigateHome,
                onReplay = { viewModel.startNewGame() }
            )
        }

        // ─── Game Over Overlay ──────────────────────────
        if (uiState.isGameOver && !uiState.isLevelComplete) {
            val context = LocalContext.current
            val activity = context as? android.app.Activity
            GameOverDialog(
                score = uiState.score,
                af3Enabled = uiState.af3Enabled,
                af8Enabled = uiState.af8Enabled,
                submitStatus = uiState.scoreSubmitStatus,
                continueCost = uiState.continueCoinCost,
                canCoinContinue = uiState.canCoinContinue,
                canRewardedContinue = uiState.canRewardedContinue,
                showGhost = uiState.modeId == ModeIds.DAILY && uiState.af8Enabled,
                onShare = {
                    viewModel.createShareIntent()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share run"))
                    }
                },
                onWatchGhost = { viewModel.loadGhostReplay() },
                onContinueCoins = { viewModel.onContinueWithCoins() },
                onContinueRewarded = {
                    if (activity != null && uiState.af9Enabled) {
                        viewModel.onContinueWithRewardedAd(activity)
                    } else {
                        viewModel.onContinueWithRewardedAd()
                    }
                },
                onRestart = { viewModel.startNewGame() }
            )
        }

        if (uiState.showGhostOverlay && uiState.ghostFrames.isNotEmpty()) {
            GhostOverlayBanner(
                frameCount = uiState.ghostFrames.size,
                onDismiss = { viewModel.dismissGhostOverlay() }
            )
        }

        if (uiState.showInsufficientFunds) {
            val activity = LocalContext.current as? android.app.Activity
            InsufficientFundsSheet(
                onShop = {
                    viewModel.dismissInsufficientFunds()
                    onNavigateShop()
                },
                onWatchAd = {
                    if (activity != null && uiState.af9Enabled) {
                        viewModel.onWatchFundsAd(activity)
                    } else {
                        viewModel.onRewardedCoinsStub()
                    }
                },
                onDismiss = { viewModel.dismissInsufficientFunds() }
            )
        }

        uiState.confirmBooster?.let { type ->
            BoosterConfirmSheet(
                type = type,
                cost = uiState.boosterButtons.firstOrNull { it.type == type }?.cost ?: 0,
                onConfirm = { viewModel.confirmPendingBooster() },
                onDismiss = { viewModel.dismissConfirmBooster() }
            )
        }

        // ─── Pause Overlay ──────────────────────────────
        if (uiState.isPaused) {
            PauseDialog(
                onResume = { viewModel.onResume() },
                onHome = onNavigateHome
            )
        }
        } // statusBars padded content
    }
}

@Composable
private fun PauseDialog(
    onResume: () -> Unit,
    onHome: () -> Unit
) {
    androidx.activity.compose.BackHandler { onResume() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            // Consume all touches so the game board can't be interacted with while paused
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GameColors.WoodDark.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(GameColors.CoinGold, GameColors.TileGold, GameColors.CoinGold)
                )
            ),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pause icon badge
                Surface(
                    shape = CircleShape,
                    color = GameColors.WoodMid.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, GameColors.CoinGold.copy(alpha = 0.6f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GameIcon(
                            resId = GameIcons.Pause,
                            contentDescription = null,
                            tint = GameColors.CoinGold,
                            size = 30.dp
                        )
                    }
                }

                Text(
                    text = "GAME PAUSED",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = GameColors.CoinGold
                )

                Text(
                    text = "Take a breath — your board is safe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GameColors.TextWhite.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Resume button
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.CoinGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    GameIcon(
                        resId = GameIcons.Play,
                        contentDescription = null,
                        tint = Color.Black,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Go Home button
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GameColors.WoodLight.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    GameIcon(
                        resId = GameIcons.Back,
                        contentDescription = null,
                        tint = GameColors.TextWhite.copy(alpha = 0.8f),
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GO HOME",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = GameColors.TextWhite.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCompleteDialog(
    level: Int,
    score: Long,
    starsEarned: Int,
    af8Enabled: Boolean = false,
    submitStatus: String? = null,
    canDoubleCoins: Boolean = false,
    onShare: () -> Unit = {},
    onDoubleCoins: () -> Unit = {},
    onNextLevel: () -> Unit,
    onNextLevelFresh: (() -> Unit)? = null,
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
                            GameIcon(
                                resId = GameIcons.Check,
                                contentDescription = "Success",
                                tint = Color.Black,
                                size = 36.dp
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.level_complete_level, level),
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
                            StarIcon(
                                tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.25f),
                                size = (36f * starScale).dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoinIcon(size = 20.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+50 Coins Earned",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GameColors.CoinGold
                                )
                            }
                        }
                    }

                    if (submitStatus != null) {
                        Text(
                            text = submitStatus,
                            color = GameColors.TextWhite.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (af8Enabled) {
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.game_over_share), color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (canDoubleCoins) {
                        OutlinedButton(
                            onClick = onDoubleCoins,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.watch_ad_double_coins), color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
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
                            GameIcon(
                                resId = GameIcons.Play,
                                contentDescription = null,
                                tint = Color.Black,
                                size = 24.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.level_complete_continue),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (onNextLevelFresh != null) {
                            OutlinedButton(
                                onClick = onNextLevelFresh,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, GameColors.WoodLight),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.level_complete_fresh_board),
                                    color = GameColors.TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateHome,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, GameColors.WoodLight),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.level_complete_map),
                                color = GameColors.TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
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
    objectiveChips: List<ObjectiveProgress> = emptyList(),
    showTarget: Boolean = true,
    showTimer: Boolean = false,
    timeRemainingMs: Long = 0L,
    onPauseClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPauseClick) {
                GameIcon(
                    resId = GameIcons.Pause,
                    contentDescription = stringResource(R.string.cd_pause),
                    tint = GameColors.TextWhite,
                    size = 24.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val header = when {
                    showTimer -> {
                        val totalSec = (timeRemainingMs / 1000L).coerceAtLeast(0L)
                        val m = totalSec / 60
                        val s = totalSec % 60
                        "Time %d:%02d".format(m, s)
                    }
                    objectiveChips.isNotEmpty() -> "Level $level"
                    showTarget -> "Level $level • Target: $targetValue"
                    else -> "Level $level"
                }
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (showTimer && timeRemainingMs < 30_000L) {
                        GameColors.Error
                    } else {
                        GameColors.TextWhite.copy(alpha = 0.8f)
                    }
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
                    CoinIcon(size = 18.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$coins",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GameColors.CoinGold
                    )
                }
            }
        }

        if (objectiveChips.isNotEmpty()) {
            ObjectiveChipsRow(chips = objectiveChips)
        }
    }
}

@Composable
private fun ObjectiveChipsRow(
    chips: List<ObjectiveProgress>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        chips.forEach { chip ->
            ObjectiveChip(progress = chip)
        }
    }
}

@Composable
private fun ObjectiveChip(
    progress: ObjectiveProgress
) {
    val tint = when {
        progress.isFailed -> GameColors.Error
        progress.isComplete -> GameColors.Success
        else -> GameColors.TextWhite.copy(alpha = 0.85f)
    }
    val bg = when {
        progress.isFailed -> GameColors.Error.copy(alpha = 0.2f)
        progress.isComplete -> GameColors.Success.copy(alpha = 0.2f)
        else -> GameColors.WoodLight.copy(alpha = 0.25f)
    }
    val text = when {
        progress.isComplete -> "${progress.label} ✓"
        else -> "${progress.label} ${progress.current}/${progress.target}"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 2
        )
    }
}

private fun dispatchBooster(viewModel: GameViewModel, type: BoosterType) {
    when (type) {
        BoosterType.UNDO -> viewModel.onBoosterUndo()
        BoosterType.SWAP -> viewModel.onBoosterSwap()
        BoosterType.RANDOMIZE -> viewModel.onBoosterShuffle()
        BoosterType.REMOVE -> viewModel.onBoosterRemoveHighest()
        BoosterType.HAMMER -> viewModel.onBoosterHammer()
        BoosterType.VALUE_UP -> viewModel.onBoosterValueUp()
        BoosterType.MAGNET -> viewModel.onBoosterMagnet()
        BoosterType.TIME_FREEZE -> viewModel.onBoosterTimeFreeze()
        BoosterType.CONTINUE -> Unit
    }
}

@Composable
private fun GameHintRow(uiState: GameUiState, viewModel: GameViewModel) {
    if (!uiState.af4Enabled) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.showDeadlockWarning) {
            Text(
                text = stringResource(R.string.game_deadlock_warning),
                color = GameColors.Warning,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.dismissDeadlockWarning() }
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Button(
            onClick = { viewModel.onHintClick() },
            enabled = uiState.canUseHint,
            colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
        ) {
            Text(
                text = stringResource(R.string.game_hint, Constants.HINT_COST),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (uiState.canWatchHintAd) {
            val activity = LocalContext.current as? android.app.Activity
            TextButton(
                onClick = { activity?.let { viewModel.onHintWithRewardedAd(it) } },
                enabled = activity != null
            ) {
                Text(
                    stringResource(R.string.game_hint_ad),
                    color = GameColors.CoinGold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun GameBoardSection(
    uiState: GameUiState,
    boardTheme: com.mergeseven.game.ui.theme.BoardTheme,
    juiceState: JuiceUiState,
    juice: JuiceController,
    focusedCell: HexCoord?,
    onFocusedCellChange: (HexCoord?) -> Unit,
    onBoardBounds: (Rect) -> Unit,
    onCellTapped: (HexCoord) -> Unit,
    onPlace: (HexCoord) -> Unit,
    onSelectTraySlot: (Int) -> Unit,
    onClearDropSnap: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val (shakeX, shakeY) = rememberShakeOffset(
        amplitudePx = juiceState.shakeAmplitudePx,
        reduceMotion = juiceState.reduceMotion
    )
    Box(
        modifier = modifier
            .shakeGraphics(shakeX, shakeY)
            .onGloballyPositioned { coordinates ->
                onBoardBounds(coordinates.boundsInRoot())
            },
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = GameColors.CoinGold)
        } else {
            val boardPadding = if (uiState.largeTouchTargets) 4f else 8f
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HexBoardCanvas(
                    playableCells = uiState.boardCells,
                    tiles = uiState.tiles,
                    boardRadius = uiState.boardRadius,
                    cellModifiers = uiState.cellModifiers,
                    hoveredCells = uiState.hoveredCells,
                    hintCells = uiState.hintCells,
                    boardTheme = boardTheme,
                    focusedCell = if (uiState.af11Enabled) focusedCell else null,
                    colourblindMode = uiState.colourblindMode,
                    largeTouchTargets = uiState.largeTouchTargets,
                    modifier = if (uiState.pendingBooster != null) {
                        Modifier.pointerInput(uiState.pendingBooster, uiState.boardCells, boardPadding) {
                            detectTapGestures { offset ->
                                val boardW = size.width.toFloat()
                                val boardH = size.height.toFloat()
                                val hexSize = HexGeometry.calculateHexSize(
                                    uiState.boardRadius,
                                    boardW,
                                    boardH,
                                    boardPadding
                                )
                                val cell = HexGeometry.nearestCell(
                                    offset.x,
                                    offset.y,
                                    hexSize,
                                    boardW / 2f,
                                    boardH / 2f,
                                    uiState.boardCells,
                                    hexSize * 2.2f
                                )
                                if (cell != null) onCellTapped(cell)
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                ParticleCanvas(
                    juice = juice,
                    boardRadius = uiState.boardRadius,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp)
                )
                DropTrailOverlay(
                    request = juiceState.dropSnap,
                    reduceMotion = juiceState.reduceMotion,
                    onFinished = onClearDropSnap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp)
                )
                ComboBanner(
                    visible = juiceState.showComboBanner,
                    chainLength = juiceState.comboLength,
                    reduceMotion = juiceState.reduceMotion,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                if (uiState.af11Enabled) {
                    HexCellAccessibilityOverlay(
                        playableCells = uiState.boardCells,
                        tiles = uiState.tiles,
                        boardRadius = uiState.boardRadius,
                        focusedCell = focusedCell,
                        selectedSlotIndex = uiState.selectedSlotIndex,
                        pendingBooster = uiState.pendingBooster != null,
                        largeTouchTargets = uiState.largeTouchTargets,
                        onFocusedCellChange = onFocusedCellChange,
                        onPlace = onPlace,
                        onBoosterTarget = onCellTapped,
                        onSelectTraySlot = onSelectTraySlot,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun HexBoardCanvas(
    playableCells: Set<HexCoord>,
    tiles: Map<HexCoord, TileUi>,
    boardRadius: Int,
    cellModifiers: Map<HexCoord, CellModifier> = emptyMap(),
    hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList(),
    hintCells: List<HexCoord> = emptyList(),
    boardTheme: com.mergeseven.game.ui.theme.BoardTheme = BoardThemes.Wood,
    focusedCell: HexCoord? = null,
    colourblindMode: ColourblindMode = ColourblindMode.OFF,
    largeTouchTargets: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val boardPad = if (largeTouchTargets) 8.dp else 16.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(boardPad)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val hexSize = HexGeometry.calculateHexSize(
            boardRadius = boardRadius,
            availableWidth = size.width,
            availableHeight = size.height,
            padding = if (largeTouchTargets) 4f else 8f
        )

        for (cell in playableCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            drawHexCell(px, py, hexSize, boardTheme.cellEmpty)
            cellModifiers[cell]?.let { mod ->
                drawCellModifierUnderlay(px, py, hexSize, mod)
            }
        }

        for (cell in hintCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            drawHexCell(px, py, hexSize, boardTheme.cellHint)
        }

        for ((coord, tile) in tiles) {
            val (px, py) = HexGeometry.hexToPixel(coord, hexSize, centerX, centerY)
            drawHexTile(
                px,
                py,
                hexSize,
                tile.color,
                tile.value,
                tile.trait,
                tile.freezeStage,
                tile.multiplierFactor,
                showShapeCue = colourblindMode != ColourblindMode.OFF,
                fontScale = fontScale
            )
            cellModifiers[coord]?.let { mod ->
                if (mod.type != CellModifierType.SPAWN_VENT) {
                    drawCellModifierUnderlay(px, py, hexSize * 0.55f, mod)
                }
            }
        }

        for ((cell, isValid) in hoveredCells) {
            val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
            val hoverColor = if (isValid) boardTheme.cellHighlight else boardTheme.cellInvalid
            drawHexCell(px, py, hexSize, hoverColor)
        }

        focusedCell?.let { cell ->
            if (cell in playableCells) {
                val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
                val path = hexPath(px, py, hexSize * 0.98f)
                drawPath(path, GameColors.CoinGold, style = Stroke(width = 4f))
            }
        }
    }
}

private fun DrawScope.drawCellModifierUnderlay(
    centerX: Float,
    centerY: Float,
    size: Float,
    modifier: CellModifier
) {
    when (modifier.type) {
        CellModifierType.SCORE_PAD -> {
            val path = hexPath(centerX, centerY, size * 0.92f)
            drawPath(path, Color(0x66FFD54A), style = Fill)
        }

        CellModifierType.SPAWN_VENT -> {
            drawCircle(
                color = Color(0xAA4FC3F7),
                radius = size * 0.28f,
                center = Offset(centerX, centerY),
                style = Stroke(width = size * 0.08f)
            )
            drawCircle(
                color = Color(0x554FC3F7),
                radius = size * 0.12f,
                center = Offset(centerX, centerY)
            )
        }

        CellModifierType.LOCKED -> {
            val s = size * 0.18f
            drawLine(
                Color.White.copy(alpha = 0.85f),
                Offset(centerX - s, centerY - s),
                Offset(centerX + s, centerY + s),
                strokeWidth = size * 0.07f
            )
            drawLine(
                Color.White.copy(alpha = 0.85f),
                Offset(centerX + s, centerY - s),
                Offset(centerX - s, centerY + s),
                strokeWidth = size * 0.07f
            )
        }
    }
}

@Composable
private fun ThreeOptionBottomTray(
    trayPieces: List<TilePiece?>,
    selectedSlotIndex: Int,
    draggingSlotIndex: Int?,
    tileTheme: com.mergeseven.game.ui.theme.TileTheme,
    colourblindMode: ColourblindMode = ColourblindMode.OFF,
    largeTouchTargets: Boolean = false,
    onSelectTraySlot: (Int) -> Unit = {},
    onRotatePiece: (Int) -> Unit = {},
    onTrayPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val trayHeight = if (largeTouchTargets) 120.dp else 100.dp
    // Whether the selected slot actually has a piece to rotate
    val canRotate = trayPieces.getOrNull(selectedSlotIndex) != null

    Surface(
        modifier = modifier.onGloballyPositioned { coords ->
            onTrayPositioned(coords.boundsInRoot())
        },
        shape = RoundedCornerShape(20.dp),
        color = GameColors.WoodMid.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(2.dp, GameColors.WoodLight)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.game_tray_hint).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.CoinGold.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
                // ↻ Rotate button — rotates the currently selected tray piece
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (canRotate)
                        GameColors.CoinGold.copy(alpha = 0.18f)
                    else
                        GameColors.WoodLight.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canRotate) GameColors.CoinGold.copy(alpha = 0.55f)
                        else GameColors.WoodLight.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .clickable(enabled = canRotate) {
                            onRotatePiece(selectedSlotIndex)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "↻",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = if (canRotate) GameColors.CoinGold
                            else GameColors.TextWhite.copy(alpha = 0.35f)
                        )
                        Text(
                            text = "ROTATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = if (canRotate) GameColors.CoinGold
                            else GameColors.TextWhite.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                        tileTheme = tileTheme,
                        colourblindMode = colourblindMode,
                        onClick = { onSelectTraySlot(index) },
                        modifier = Modifier
                            .weight(1f)
                            .height(trayHeight)
                            .heightIn(min = 48.dp)
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
    tileTheme: com.mergeseven.game.ui.theme.TileTheme,
    colourblindMode: ColourblindMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) GameColors.CoinGold else GameColors.WoodLight.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val bgColor = if (isSelected) GameColors.WoodLight.copy(alpha = 0.35f) else GameColors.WoodDark.copy(alpha = 0.4f)
    val slotDesc = stringResource(R.string.game_tray_slot, slotIndex + 1)

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = slotDesc
            }
            .clickable(onClick = onClick)
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
                    drawHexTile(
                        px,
                        py,
                        hexSize,
                        tileTheme.tileColor(cell.value),
                        cell.value,
                        showShapeCue = colourblindMode != ColourblindMode.OFF
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp),
                shape = CircleShape,
                color = if (isSelected) GameColors.CoinGold else GameColors.WoodLight
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                    Text(
                        text = "${slotIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        } else if (piece == null || isBeingDragged) {
            Text(
                text = stringResource(
                    if (isBeingDragged) R.string.game_tray_dragging else R.string.game_tray_empty
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = GameColors.TextWhite.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun GameOverDialog(
    score: Long,
    af3Enabled: Boolean,
    af8Enabled: Boolean = false,
    submitStatus: String? = null,
    continueCost: Int,
    canCoinContinue: Boolean,
    canRewardedContinue: Boolean,
    showGhost: Boolean = false,
    onShare: () -> Unit = {},
    onWatchGhost: () -> Unit = {},
    onContinueCoins: () -> Unit,
    onContinueRewarded: () -> Unit,
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
                    text = stringResource(R.string.game_over_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )

                Text(
                    text = stringResource(R.string.game_over_final_score, score),
                    style = MaterialTheme.typography.titleLarge,
                    color = GameColors.CoinGold
                )

                if (submitStatus != null) {
                    Text(
                        text = submitStatus,
                        color = GameColors.TextWhite.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (af8Enabled) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.game_over_share), color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
                    }
                }
                if (showGhost) {
                    TextButton(onClick = onWatchGhost) {
                        Text(stringResource(R.string.game_over_watch_ghost), color = GameColors.TextWhite)
                    }
                }

                if (af3Enabled && canCoinContinue) {
                    Button(
                        onClick = onContinueCoins,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.Success)
                    ) {
                        Text(
                            text = stringResource(R.string.game_over_continue_cost, continueCost),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                if (af3Enabled && canRewardedContinue) {
                    OutlinedButton(onClick = onContinueRewarded) {
                        Text(
                            text = stringResource(R.string.game_over_watch_ad),
                            color = GameColors.TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                ) {
                    Text(
                        text = stringResource(R.string.game_over_play_again),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostOverlayBanner(
    frameCount: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameColors.WoodDark.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GameColors.CoinGold)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ghost_loaded, frameCount),
                    color = GameColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close), color = GameColors.CoinGold)
                }
            }
        }
    }
}

@Composable
private fun InsufficientFundsSheet(
    onShop: () -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Not enough coins",
                    style = MaterialTheme.typography.titleLarge,
                    color = GameColors.TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onWatchAd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                ) {
                    Text("WATCH AD (+50)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onShop, modifier = Modifier.fillMaxWidth()) {
                    Text("OPEN SHOP", color = GameColors.TextWhite)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("DISMISS", color = GameColors.TextWhite.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun BoosterConfirmSheet(
    type: BoosterType,
    cost: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Use ${type.name.replace('_', ' ')} for $cost coins?",
                    color = GameColors.TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = GameColors.TextWhite)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                    ) {
                        Text(stringResource(R.string.confirm), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
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
    value: Int,
    trait: TileTrait = TileTrait.NORMAL,
    freezeStage: Int = 0,
    multiplierFactor: Int = 1,
    showShapeCue: Boolean = false,
    fontScale: Float = 1f
) {
    val tileSize = size * 0.9f

    val shadowPath = hexPath(centerX + 2f, centerY + 3f, tileSize)
    drawPath(shadowPath, Color.Black.copy(alpha = 0.3f), style = Fill)

    val mainPath = hexPath(centerX, centerY, tileSize)
    drawPath(mainPath, color, style = Fill)
    drawPath(mainPath, color.copy(alpha = 0.7f), style = Stroke(width = 2f))

    val highlightPath = hexPath(centerX, centerY - 1f, tileSize * 0.85f)
    drawPath(highlightPath, Color.White.copy(alpha = 0.15f), style = Fill)

    drawTraitSilhouette(centerX, centerY, tileSize, trait, freezeStage, multiplierFactor)
    if (showShapeCue) {
        drawValueShapeCue(centerX, centerY, tileSize, value)
    }

    drawContext.canvas.nativeCanvas.apply {
        val baseText = when {
            value >= 1000 -> tileSize * 0.35f
            value >= 100 -> tileSize * 0.45f
            else -> tileSize * 0.55f
        }
        val textSize = (baseText * fontScale).coerceAtMost(tileSize * 0.72f)
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
