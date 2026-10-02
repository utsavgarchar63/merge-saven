package com.mergeseven.game.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.toArgb
import com.mergeseven.game.ui.components.WoodPanel
import com.mergeseven.game.ui.components.GoldButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mergeseven.game.ui.feel.MergeFlight
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
    val fontContext = LocalContext.current
    remember(fontContext) {
        tileTypeface = com.mergeseven.game.core.audio.GameFont.bold(fontContext)
        tileTextCache.evictAll()
        true
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val juiceState by viewModel.juiceUiState.collectAsStateWithLifecycle()
    val boardTheme = BoardThemes.of(if (uiState.modeId == ModeIds.ZEN) "zen" else uiState.boardThemeId)
    val tileTheme = TileThemes.of(uiState.tileThemeId).withColourblindMode(uiState.colourblindMode)
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactHelp = configuration.screenHeightDp < 700 || LocalDensity.current.fontScale > 1.2f
    var showTutorialHelp by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.tutorialActive, uiState.animatedGuideSeen, uiState.isLoading) {
        if (uiState.tutorialActive && !uiState.animatedGuideSeen && !uiState.isLoading) showTutorialHelp = true
    }
    val tutorialMessage = when (uiState.tutorialStep) {
        0 -> "Tap a tray piece, then tap the board to place it. You can also drag a piece onto the board."
        1 -> "Tap Rotate to turn the selected piece before placing it."
        else -> "Connect three matching numbers: 2 + 2 + 2 becomes 4. Chains merge again!"
    }
    val boardPadding = if (uiState.largeTouchTargets) 4f else 8f
    val fingerLiftPx = with(LocalDensity.current) { 64.dp.toPx() }
    // Portrait tablets have enough height for the tray below a larger board.
    val useSidePane = isLandscape || configuration.screenWidthDp >= 840

    // Flush the board to disk whenever the screen leaves the foreground; the debounced autosave
    // may still be pending, and the process can be killed at any time after this point.
    LifecycleStartEffect(viewModel) {
        viewModel.onForeground()
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

    LaunchedEffect(uiState.achievementToast) {
        if (uiState.achievementToast != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissAchievementToast()
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
            .pointerInput(uiState.trayPieces, uiState.isPaused, uiState.adBusy, uiState.isLevelComplete, uiState.isGameOver, rootWindowOffset, trayBounds, layoutDirection) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (uiState.isPaused || uiState.adBusy || uiState.isLevelComplete || uiState.isGameOver) continue
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

                        // Keep taps available to selection and rotation buttons. Start a drag
                        // only after touch slop, then consume movement.
                        var dragStarted = false
                        viewModel.onSelectTraySlot(slotIdx)

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            val currentGlobalPos = change.position + rootWindowOffset
                            if (!dragStarted && change.pressed &&
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                dragStarted = true
                                draggingSlotIndex = slotIdx
                                dragGlobalPosition = currentGlobalPos
                            }

                            if (!change.pressed) {
                                // Finger released anywhere on screen!
                                val bounds = boardBounds
                                val activeSlot = draggingSlotIndex

                                if (bounds != null && activeSlot != null) {
                                    // Touch offset above the finger so piece centers right above finger tip
                                    val targetX = currentGlobalPos.x - bounds.left
                                    val targetY = currentGlobalPos.y - bounds.top - fingerLiftPx

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

                            if (!dragStarted) continue
                            change.consume()
                            dragGlobalPosition = currentGlobalPos

                            // Update board cell hover preview in real-time
                            val bounds = boardBounds
                            val activeSlot = draggingSlotIndex
                            if (bounds != null && activeSlot != null) {
                                val targetX = currentGlobalPos.x - bounds.left
                                val targetY = currentGlobalPos.y - bounds.top - fingerLiftPx

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
                modeId = uiState.modeId,
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

        if (uiState.tutorialActive && !uiState.isLevelComplete && !uiState.isGameOver && !uiState.isPaused) {
            Surface(onClick = { showTutorialHelp = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                color = GameColors.WoodDark, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (compactHelp) when (uiState.tutorialStep) {
                        0 -> "1 · Place"
                        1 -> "2 · Rotate"
                        else -> "3 · Merge"
                    } else "${uiState.tutorialStep + 1} · $tutorialMessage",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton({ viewModel.skipTutorial() }) { Text("Skip") }
                }
            }
        }
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
                        .navigationBarsPadding()
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
                         onCellHovered = viewModel::onCellHover,
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
                            .padding(start = 8.dp).verticalScroll(rememberScrollState())
                    ) {
                        BoosterTray(
                            buttons = uiState.boosterButtons,
                            af3Enabled = uiState.af3Enabled,
                            largeTouchTargets = uiState.largeTouchTargets,
                            verticalLayout = true,
                            onBooster = { type -> dispatchBooster(viewModel, type) },
                            extraContent = { dismiss -> GameHintRow(uiState = uiState, viewModel = viewModel, onAction = dismiss) },
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
                            reduceMotion = juiceState.reduceMotion,
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
                         onCellHovered = viewModel::onCellHover,
                    onPlace = { cell -> viewModel.onDropOnCell(cell) },
                    onSelectTraySlot = viewModel::onSelectTraySlot,
                    onClearDropSnap = viewModel::clearDropSnap,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))


                BoosterTray(
                    buttons = uiState.boosterButtons,
                    af3Enabled = uiState.af3Enabled,
                    largeTouchTargets = uiState.largeTouchTargets,
                    onBooster = { type -> dispatchBooster(viewModel, type) },
                            extraContent = { dismiss -> GameHintRow(uiState = uiState, viewModel = viewModel, onAction = dismiss) },
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
                    reduceMotion = juiceState.reduceMotion,
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
            Snackbar(containerColor = GameColors.WoodDark, contentColor = GameColors.TextWhite,
                actionContentColor = GameColors.CoinGold,
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
                    val floatY = currentDragPos.y - fingerLiftPx
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
                isCampaign = uiState.modeId == ModeIds.CAMPAIGN,
                score = uiState.score,
                starsEarned = uiState.starsEarned,
                af8Enabled = uiState.af8Enabled,
                canDoubleCoins = uiState.canDoubleCoins,
                baseRewardCoins = uiState.baseRewardCoins, adBusy = uiState.adBusy,
                onShare = {
                    viewModel.createShareIntent()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share run"))
                    }
                },
                onDoubleCoins = { activity?.let { viewModel.onDoubleCoinsAd(it) } },
                onNextLevel = { viewModel.nextLevelAfterAd(activity, true) },
                onNextLevelFresh = { viewModel.nextLevelAfterAd(activity, false) },
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
                timedOut = uiState.showTimer && uiState.timeRemainingMs <= 0L,
                baseRewardCoins = uiState.baseRewardCoins, adBusy = uiState.adBusy, onHome = onNavigateHome,
                af3Enabled = uiState.af3Enabled,
                af8Enabled = uiState.af8Enabled,
                continueCost = uiState.continueCoinCost,
                canCoinContinue = uiState.canCoinContinue,
                canRewardedContinue = uiState.canRewardedContinue,
                onShare = {
                    viewModel.createShareIntent()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share run"))
                    }
                },
                onContinueCoins = { viewModel.onContinueWithCoins() },
                onContinueRewarded = {
                    if (activity != null && uiState.af9Enabled) {
                        viewModel.onContinueWithRewardedAd(activity)
                    }
                },
                onRestart = { viewModel.startNewGame() }
            )
        }



        if (uiState.showInsufficientFunds) {
            val activity = LocalContext.current as? android.app.Activity
            InsufficientFundsSheet(
                canWatchAd = activity != null && uiState.af9Enabled && !uiState.suppressAds && !uiState.adBusy,
                onShop = {
                    viewModel.dismissInsufficientFunds()
                    onNavigateShop()
                },
                onWatchAd = {
                    if (activity != null && uiState.af9Enabled) {
                        viewModel.onWatchFundsAd(activity)
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
        if (uiState.isPaused && !uiState.adBusy) {
            val sound by viewModel.pauseSoundEnabled.collectAsStateWithLifecycle(initialValue = true)
            val music by viewModel.pauseMusicEnabled.collectAsStateWithLifecycle(initialValue = true)
            val haptics by viewModel.pauseHapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
            PauseDialog(
                soundEnabled = sound, musicEnabled = music, hapticsEnabled = haptics,
                onHaptics = viewModel::togglePauseHaptics,
                onResume = { viewModel.onResume() },
                onHome = { viewModel.onStopped(); onNavigateHome() },
                onRestart = { viewModel.startNewGame(); viewModel.onResume() },
                onSound = { viewModel.togglePauseSound() }, onMusic = { viewModel.togglePauseMusic() },
                onHelp = { viewModel.setGuidePage(0); showTutorialHelp = true }
            )
        }
        if (showTutorialHelp) AnimatedGameGuide(
            page = uiState.animatedGuidePage,
            reduceMotion = juiceState.reduceMotion,
            onPage = viewModel::setGuidePage,
            onFinish = { viewModel.finishAnimatedGuide(); showTutorialHelp = false }
        )
        uiState.adStatusMessage?.let { message ->
            Surface(Modifier.align(Alignment.TopCenter).padding(16.dp), color = GameColors.WoodDark,
                shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(viewModel::dismissAdStatus) { Text("OK") }
                }
            }
        }

        } // statusBars padded content
    }
}

@Composable
private fun PauseDialog(onResume: () -> Unit, onHome: () -> Unit, onRestart: () -> Unit,
    onSound: () -> Unit, onMusic: () -> Unit, onHelp: () -> Unit,
    soundEnabled: Boolean, musicEnabled: Boolean, hapticsEnabled: Boolean, onHaptics: () -> Unit) {
    var confirmRestart by remember { mutableStateOf(false) }
    ResultSurface {
        Text("Take a breath", style = MaterialTheme.typography.headlineMedium)
        Text("Your board is safe. Pick up where you left off.")
        GoldButton("Resume", onClick = onResume)
        listOf(Triple("Sound", soundEnabled, onSound), Triple("Music", musicEnabled, onMusic),
            Triple("Haptics", hapticsEnabled, onHaptics)).forEach { (label, enabled, toggle) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$label · ${if (enabled) "On" else "Off"}", Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { toggle() },
                    modifier = Modifier.semantics { contentDescription = "$label ${if (enabled) "on" else "off"}" })
            }
        }
        TextButton(onHelp, Modifier.fillMaxWidth()) {
            GameIcon(R.drawable.icon_help, null, tint = GameColors.CoinGold)
            Spacer(Modifier.width(8.dp)); Text("How to play")
        }
        OutlinedButton({ confirmRestart = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            GameIcon(R.drawable.icon_retry, null, tint = GameColors.CoinGold)
            Spacer(Modifier.width(8.dp)); Text("Restart this run")
        }
        TextButton(onHome, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save and go Home") }
    }
    if (confirmRestart) AlertDialog(onDismissRequest = { confirmRestart = false },
        title = { Text("Start again?") }, text = { Text("This board will be replaced. Your coins and progress are kept.") },
        confirmButton = { TextButton({ confirmRestart = false; onRestart() }) { Text("Restart") } },
        dismissButton = { TextButton({ confirmRestart = false }) { Text("Keep playing") } })
}
@Composable
private fun ResultSurface(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            WoodPanel(content = content)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LevelCompleteDialog(level: Int, isCampaign: Boolean, score: Long, starsEarned: Int,
    af8Enabled: Boolean = false, canDoubleCoins: Boolean = false,
    baseRewardCoins: Int = 0, adBusy: Boolean = false, onShare: () -> Unit = {}, onDoubleCoins: () -> Unit = {},
    onNextLevel: () -> Unit, onNextLevelFresh: (() -> Unit)? = null, onNavigateHome: () -> Unit, onReplay: () -> Unit) {
    ResultSurface {
        Image(painterResource(R.drawable.art_celebration_v3), null, Modifier.size(80.dp).align(Alignment.CenterHorizontally))
        Text("Beautiful merge!", style = MaterialTheme.typography.headlineMedium)
        Text(if (isCampaign) "Level $level complete · $starsEarned stars" else "Challenge complete")
        Text("Score $score", style = MaterialTheme.typography.titleLarge, color = GameColors.CoinGold)
        Text("+$baseRewardCoins coins earned")
        GoldButton(if (adBusy) "Finishing…" else if (isCampaign) "Next level" else "Back to Home", !adBusy, if (isCampaign) onNextLevel else onNavigateHome)
        if (canDoubleCoins) OutlinedButton(onDoubleCoins, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !adBusy) {
            Text("Watch ad · Get $baseRewardCoins extra coins")
        }
        if (isCampaign && onNextLevelFresh != null) TextButton(onNextLevelFresh, Modifier.fillMaxWidth(), enabled = !adBusy) { Text("Next level with a fresh board") }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onReplay, enabled = !adBusy) { Text("Replay") }
            TextButton(onNavigateHome, enabled = !adBusy) { Text("Home") }
            if (af8Enabled) TextButton(onShare, enabled = !adBusy) {
                GameIcon(R.drawable.icon_share, null, tint = GameColors.CoinGold, size = 18.dp)
                Spacer(Modifier.width(4.dp)); Text("Share")
            }
        }
    }
}

@Composable
private fun GameTopBar(
    modeId: String,
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
    val compact = LocalConfiguration.current.screenHeightDp < 700 && LocalDensity.current.fontScale > 1.2f
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
                val modeLabel = when (modeId) {
                    ModeIds.ENDLESS -> "Endless"
                    ModeIds.ZEN -> "Zen"
                    ModeIds.DAILY -> "Daily puzzle"
                    ModeIds.WEEKLY -> "Weekly challenge"
                    else -> "Level $level"
                }
                val header = when {
                    showTimer -> {
                        val totalSec = (timeRemainingMs / 1000L).coerceAtLeast(0L)
                        val m = totalSec / 60
                        val s = totalSec % 60
                        "Time %d:%02d".format(m, s)
                    }
                    objectiveChips.isNotEmpty() -> modeLabel
                    showTarget -> "$modeLabel · Target: $targetValue"
                    else -> modeLabel
                }
                Text(
                    text = if (compact) "$header · $score" else header,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (showTimer && timeRemainingMs < 30_000L) {
                        GameColors.Error
                    } else {
                        GameColors.TextWhite.copy(alpha = 0.8f)
                    }
                )
                if (!compact) Text(
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
private fun GameHintRow(uiState: GameUiState, viewModel: GameViewModel, onAction: () -> Unit = {}) {
    if (!uiState.af4Enabled || uiState.tutorialActive) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (uiState.showDeadlockWarning) Text(stringResource(R.string.game_deadlock_warning),
            color = GameColors.Warning, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { viewModel.dismissDeadlockWarning() })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction(); viewModel.onHintClick() }, enabled = uiState.canUseHint && !uiState.adBusy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)) {
                Text(stringResource(R.string.game_hint, uiState.hintCoinCost), color = Color.Black,
                    style = MaterialTheme.typography.labelMedium)
            }
            if (uiState.canWatchHintAd) {
                val activity = LocalContext.current as? android.app.Activity
                OutlinedButton(onClick = { onAction(); activity?.let(viewModel::onHintWithRewardedAd) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), enabled = activity != null && !uiState.adBusy) {
                    Text(stringResource(R.string.game_hint_ad), style = MaterialTheme.typography.labelMedium)
                }
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
    onCellHovered: (HexCoord?) -> Unit,
    onPlace: (HexCoord) -> Unit,
    onSelectTraySlot: (Int) -> Unit,
    onClearDropSnap: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val shakeOffset = rememberShakeOffset(
        amplitudePx = juiceState.shakeAmplitudePx,
        reduceMotion = juiceState.reduceMotion
    )
    Box(
        modifier = modifier
            .shakeGraphics(shakeOffset).clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = GameColors.CoinGold)
        } else {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val boardSize = minOf(maxWidth, maxHeight, 600.dp).coerceAtLeast(1.dp)
            Box(Modifier.size(boardSize), contentAlignment = Alignment.Center) {
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
                    reduceMotion = juiceState.reduceMotion,
                    mergeFlights = juiceState.mergeFlights,
                    motionRevision = juiceState.revision,
                    onCellTapped = onCellTapped,
                    onCellHovered = onCellHovered,
                    onBoardBounds = onBoardBounds

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
    reduceMotion: Boolean = false,
    mergeFlights: List<MergeFlight> = emptyList(),
    motionRevision: Long = 0L,
    onCellTapped: (HexCoord) -> Unit,
    onCellHovered: (HexCoord?) -> Unit,
    onBoardBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val boardPad = if (largeTouchTargets) 8.dp else 16.dp
    val arrival = remember { Animatable(1f) }
    var previousTiles by remember { mutableStateOf(tiles) }
    var arrivingCells by remember { mutableStateOf(emptySet<HexCoord>()) }
    val mergeProgress = remember { Animatable(1f) }
    LaunchedEffect(tiles, reduceMotion) {
        arrivingCells = tiles.keys.filterTo(mutableSetOf()) { tiles[it] != previousTiles[it] }
        previousTiles = tiles
        if (!reduceMotion && arrivingCells.isNotEmpty()) {
            arrival.snapTo(0.72f)
            arrival.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 650f))
        } else arrival.snapTo(1f)
        arrivingCells = emptySet()
    }
    LaunchedEffect(motionRevision, reduceMotion) {
        if (!reduceMotion && mergeFlights.isNotEmpty()) {
            mergeProgress.snapTo(0f)
            mergeProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        } else mergeProgress.snapTo(1f)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(boardPad)
            .onGloballyPositioned { onBoardBounds(it.boundsInRoot()) }
            .pointerInput(playableCells, boardRadius, largeTouchTargets) {
                fun cellAt(offset: Offset): HexCoord? {
                    val hexSize = HexGeometry.calculateHexSize(boardRadius, size.width.toFloat(), size.height.toFloat(), if (largeTouchTargets) 4f else 8f)
                    return HexGeometry.nearestCell(offset.x, offset.y, hexSize, size.width / 2f, size.height / 2f, playableCells, hexSize)
                }
                detectTapGestures(
                    onPress = { offset ->
                        onCellHovered(cellAt(offset))
                        tryAwaitRelease()
                        onCellHovered(null)
                    },
                    onTap = { offset -> cellAt(offset)?.let(onCellTapped) }
                )
            }
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
                hexSize * if (coord in arrivingCells) arrival.value else 1f,
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

        val mergeT = mergeProgress.value
        if (!reduceMotion && mergeT < 1f) for (flight in mergeFlights) {
            val (fx, fy) = HexGeometry.hexToPixel(flight.from, hexSize, centerX, centerY)
            val (tx, ty) = HexGeometry.hexToPixel(flight.to, hexSize, centerX, centerY)
            val flightColor = tiles[flight.from]?.color ?: TileThemes.of("classic").tileColor(flight.value)
            drawHexTile(fx + (tx - fx) * mergeT, fy + (ty - fy) * mergeT,
                hexSize * (1f - mergeT * 0.75f), flightColor, flight.value,
                showShapeCue = colourblindMode != ColourblindMode.OFF, fontScale = fontScale)
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
    reduceMotion: Boolean = false,
    onSelectTraySlot: (Int) -> Unit = {},
    onRotatePiece: (Int) -> Unit = {},
    onTrayPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val narrow = LocalConfiguration.current.screenWidthDp < 360
    val trayHeight = if (largeTouchTargets) 96.dp else if (narrow) 64.dp else 80.dp
    // Whether the selected slot actually has a piece to rotate
    val canRotate = trayPieces.getOrNull(selectedSlotIndex) != null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = GameColors.WoodMid.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(2.dp, GameColors.WoodLight)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (narrow) 6.dp else 10.dp),
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
                        .heightIn(min = 48.dp).widthIn(min = 88.dp)
                        .clickable(enabled = canRotate) {
                            onRotatePiece(selectedSlotIndex)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GameIcon(R.drawable.icon_rotate, null,
                            tint = if (canRotate) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.35f), size = 20.dp)
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
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { onTrayPositioned(it.boundsInRoot()) },
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
                        reduceMotion = reduceMotion,
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
    reduceMotion: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) GameColors.CoinGold else GameColors.WoodLight.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val bgColor = if (isSelected) GameColors.WoodLight.copy(alpha = 0.35f) else GameColors.WoodDark.copy(alpha = 0.4f)
    val slotDesc = stringResource(R.string.game_tray_slot, slotIndex + 1)
    val turn = remember { Animatable(0f) }
    var previousPiece by remember { mutableStateOf(piece) }
    LaunchedEffect(piece, reduceMotion) {
        val old = previousPiece
        previousPiece = piece
        if (!reduceMotion && old != null && piece != null && old.cells == piece.cells && old.rotation != piece.rotation) {
            val steps = (piece.rotation - old.rotation + 6) % 6
            turn.snapTo(if (steps <= 3) -steps * 60f else (6 - steps) * 60f)
            turn.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
        } else turn.snapTo(0f)
    }


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
                    val (rawX, rawY) = HexGeometry.hexToPixel(cell.offset, hexSize, centerX, centerY)
                    val angle = Math.toRadians(turn.value.toDouble())
                    val dx = rawX - centerX
                    val dy = rawY - centerY
                    val px = centerX + (dx * cos(angle) - dy * sin(angle)).toFloat()
                    val py = centerY + (dx * sin(angle) + dy * cos(angle)).toFloat()
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
@OptIn(ExperimentalLayoutApi::class)
private fun GameOverDialog(score: Long, timedOut: Boolean, af3Enabled: Boolean, af8Enabled: Boolean = false,
    continueCost: Int, canCoinContinue: Boolean, canRewardedContinue: Boolean,
    baseRewardCoins: Int = 0, adBusy: Boolean = false, onHome: () -> Unit = {},
    onShare: () -> Unit = {}, onContinueCoins: () -> Unit,
    onContinueRewarded: () -> Unit, onRestart: () -> Unit) {
    ResultSurface {
        Text("A good run", style = MaterialTheme.typography.headlineMedium)
        Text(if (timedOut) "Time's up. Try again and build your next chain."
            else "The board has no room for your next piece. Try a fresh approach.")
        Text("Score $score", style = MaterialTheme.typography.titleLarge, color = GameColors.CoinGold)
        if (baseRewardCoins > 0) Text("+$baseRewardCoins coins earned")
        GoldButton("Play again", !adBusy, onRestart)
        if (af3Enabled && canCoinContinue) OutlinedButton(onContinueCoins, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !adBusy) {
            Text("Continue · $continueCost coins or owned charge")
        }
        if (af3Enabled && canRewardedContinue) OutlinedButton(onContinueRewarded, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !adBusy) {
            Text("Watch ad · Clear space and continue")
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onHome, enabled = !adBusy) { Text("Home") }
            if (af8Enabled) TextButton(onShare, enabled = !adBusy) {
                GameIcon(R.drawable.icon_share, null, tint = GameColors.CoinGold, size = 18.dp)
                Spacer(Modifier.width(4.dp)); Text("Share")
            }
        }
    }
}

@Composable
private fun InsufficientFundsSheet(
    canWatchAd: Boolean,
    onShop: () -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Not enough coins") },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton("Watch ad · Get 50 coins", enabled = canWatchAd, onClick = onWatchAd)
                OutlinedButton(onShop, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Rewards") }
                TextButton(onDismiss, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Keep playing") }
            }
        }, containerColor = GameColors.WoodMid)
}

@Composable
private fun BoosterConfirmSheet(
    type: BoosterType,
    cost: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Use ${com.mergeseven.game.ui.shop.boosterName(type)}?") },
        text = { Text("Spend $cost coins for this booster.") },
        confirmButton = { TextButton(onConfirm) { Text("Use coins") } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = GameColors.WoodMid)
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
    drawCachedHex(centerX, centerY, size * 0.95f, color)

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
    if (!size.isFinite() || size <= 0f) return
    val tileSize = size * 0.9f

    drawCachedHex(centerX + 2f, centerY + 4f, tileSize, Color.Black.copy(alpha = 0.32f))
    drawCachedHex(centerX, centerY + 2f, tileSize, color.copy(alpha = 0.8f))
    drawCachedHex(centerX, centerY, tileSize, color)
    drawCachedHex(centerX, centerY - 1f, tileSize * 0.85f, Color.White.copy(alpha = 0.15f))

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
        val label = value.toString()
        val key = TileTextKey(value, GameColors.textOnTile(color).toArgb(), textSize.toBits(), tileSize.toBits())
        val paint = tileTextCache.get(key) ?: android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = key.color
            this.textSize = textSize
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = tileTypeface
            val measuredWidth = measureText(label)
            val maxWidth = tileSize * 1.45f
            if (measuredWidth > maxWidth && measuredWidth > 0f) this.textSize *= maxWidth / measuredWidth
            tileTextCache.put(key, this)
        }
        val textY = centerY - (paint.ascent() + paint.descent()) / 2f
        drawText(label, centerX, textY, paint)
    }
}

private data class TileTextKey(val value: Int, val color: Int, val textSize: Int, val tileSize: Int)
private var tileTypeface: android.graphics.Typeface? = null
private val tileTextCache = android.util.LruCache<TileTextKey, android.graphics.Paint>(128)
private val normalizedHex = hexPath(0f, 0f, 1f)

private fun DrawScope.drawCachedHex(x: Float, y: Float, radius: Float, color: Color) {
    if (!radius.isFinite() || radius <= 0f) return
    withTransform({ translate(x, y); scale(radius, radius, Offset.Zero) }) {
        drawPath(normalizedHex, color)
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
