package com.mergeseven.game.ui.game

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mergeseven.game.R
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.TileTrait
import kotlin.math.roundToInt

/**
 * AF11-02 / AF11-03: focusable semantics overlay for Canvas hex cells + D-pad navigation.
 */
@Composable
fun HexCellAccessibilityOverlay(
    playableCells: Set<HexCoord>,
    tiles: Map<HexCoord, TileUi>,
    boardRadius: Int,
    focusedCell: HexCoord?,
    selectedSlotIndex: Int,
    pendingBooster: Boolean,
    largeTouchTargets: Boolean,
    onFocusedCellChange: (HexCoord?) -> Unit,
    onPlace: (HexCoord) -> Unit,
    onBoosterTarget: (HexCoord) -> Unit,
    onSelectTraySlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sortedCells = remember(playableCells) {
        playableCells.sortedWith(compareBy({ it.r }, { it.q }))
    }
    val placeLabel = stringResource(R.string.game_place_action)
    val boosterLabel = stringResource(R.string.game_booster_target_action)
    val playableSet = remember(sortedCells) { sortedCells.toSet() }

    LaunchedEffect(sortedCells) {
        if (focusedCell == null && sortedCells.isNotEmpty()) {
            onFocusedCellChange(sortedCells.first())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.One -> {
                        onSelectTraySlot(0); true
                    }
                    Key.Two -> {
                        onSelectTraySlot(1); true
                    }
                    Key.Three -> {
                        onSelectTraySlot(2); true
                    }
                    Key.DirectionUp, Key.W -> {
                        moveFocus(focusedCell, sortedCells, playableSet, HexCoord(0, -1), onFocusedCellChange)
                    }
                    Key.DirectionDown, Key.S -> {
                        moveFocus(focusedCell, sortedCells, playableSet, HexCoord(0, 1), onFocusedCellChange)
                    }
                    Key.DirectionLeft, Key.A -> {
                        moveFocus(focusedCell, sortedCells, playableSet, HexCoord(-1, 0), onFocusedCellChange)
                    }
                    Key.DirectionRight, Key.D -> {
                        moveFocus(focusedCell, sortedCells, playableSet, HexCoord(1, 0), onFocusedCellChange)
                    }
                    Key.Enter, Key.DirectionCenter, Key.Spacebar -> {
                        val cell = focusedCell ?: return@onPreviewKeyEvent false
                        if (pendingBooster) onBoosterTarget(cell) else onPlace(cell)
                        true
                    }
                    Key.Escape -> {
                        onFocusedCellChange(null)
                        true
                    }
                    else -> false
                }
            }
    ) {
        val paddingPx = with(density) {
            (if (largeTouchTargets) 8.dp else 16.dp).toPx()
        }
        val boardW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boardH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val hexSize = HexGeometry.calculateHexSize(
            boardRadius = boardRadius,
            availableWidth = boardW - paddingPx * 2,
            availableHeight = boardH - paddingPx * 2,
            padding = if (largeTouchTargets) 4f else 8f
        )
        val centerX = boardW / 2f
        val centerY = boardH / 2f
        val hitDp = with(density) {
            val px = if (largeTouchTargets) hexSize * 1.85f else hexSize * 1.6f
            (px / density.density).dp
        }

        for (cell in sortedCells) {
            key(cell) {
                val (px, py) = HexGeometry.hexToPixel(cell, hexSize, centerX, centerY)
                val tile = tiles[cell]
                val description = if (tile == null) {
                    stringResource(R.string.game_cell_empty, cell.q, cell.r)
                } else {
                    val base = stringResource(R.string.game_cell_occupied, tile.value, cell.q, cell.r)
                    if (tile.trait == TileTrait.NORMAL) {
                        base
                    } else {
                        base + stringResource(R.string.game_cell_trait, tile.trait.name.lowercase())
                    }
                }
                val requester = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (px - hitDp.value * density.density / 2f).roundToInt(),
                                (py - hitDp.value * density.density / 2f).roundToInt()
                            )
                        }
                        .size(hitDp)
                        .focusRequester(requester)
                        .onFocusChanged { state ->
                            if (state.isFocused) onFocusedCellChange(cell)
                        }
                        .focusable()
                        .clearAndSetSemantics {
                            contentDescription = description
                            role = Role.Button
                            selected = cell == focusedCell
                            customActions = listOf(
                                CustomAccessibilityAction(placeLabel) {
                                    onSelectTraySlot(selectedSlotIndex)
                                    onPlace(cell)
                                    true
                                },
                                CustomAccessibilityAction(boosterLabel) {
                                    onBoosterTarget(cell)
                                    true
                                }
                            )
                        }
                )
                LaunchedEffect(focusedCell) {
                    if (focusedCell == cell) {
                        runCatching { requester.requestFocus() }
                    }
                }
            }
        }
    }
}

private fun moveFocus(
    current: HexCoord?,
    sorted: List<HexCoord>,
    playable: Set<HexCoord>,
    delta: HexCoord,
    onFocusedCellChange: (HexCoord?) -> Unit
): Boolean {
    if (sorted.isEmpty()) return false
    val start = current ?: sorted.first()
    val target = start + delta
    val next = when {
        target in playable -> target
        else -> sorted.minByOrNull { it.distanceTo(target) } ?: start
    }
    onFocusedCellChange(next)
    return true
}
