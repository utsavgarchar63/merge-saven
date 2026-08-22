package com.mergeseven.game.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.ui.theme.GameColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI State exposed to the GameScreen.
 * Maps the complex GameState into simpler UI-ready properties.
 */
data class GameUiState(
    val level: Int = 1,
    val score: Long = 0,
    val coins: Int = 0,
    val targetValue: Int = 0,
    val boardRadius: Int = Constants.DEFAULT_BOARD_RADIUS,
    val boardCells: Set<HexCoord> = emptySet(),
    val tiles: Map<HexCoord, Pair<Int, Color>> = emptyMap(),
    val currentPiece: TilePiece? = null,
    val nextPieces: List<TilePiece> = emptyList(),
    val debugSelectedCell: HexCoord? = null,
    val showDebugGrid: Boolean = true,
    val hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList() // coord to isValid
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentGameState: GameState? = null

    init {
        // Initialize with a new game for the prototype
        val initialState = gameEngine.createInitialState(
            level = 1,
            bestScore = 0
        )
        updateState(initialState)
    }

    fun onPause() {
        // Handle pause action
    }

    private fun updateState(gameState: GameState) {
        currentGameState = gameState
        
        // Map domain state to UI state
        val tilesUi = gameState.board.activeTiles().associate { tile ->
            tile.cell to Pair(tile.value, GameColors.tileColor(tile.value))
        }

        _uiState.update { currentState ->
            currentState.copy(
                level = gameState.level,
                score = gameState.score,
                coins = gameState.coins,
                targetValue = gameState.targetValue,
                boardCells = gameState.board.playableCells,
                tiles = tilesUi,
                currentPiece = gameState.currentPiece,
                nextPieces = gameState.nextPieces
            )
        }
    }

    fun onRotatePiece() {
        val state = currentGameState ?: return
        val newPiece = gameEngine.rotatePiece(state.currentPiece)
        val newState = state.copy(currentPiece = newPiece)
        updateState(newState)
    }

    private var lastHoveredOrigin: HexCoord? = null

    fun onCellHover(origin: HexCoord?) {
        val state = currentGameState ?: return
        val piece = state.currentPiece
        lastHoveredOrigin = origin

        if (origin == null) {
            _uiState.update { it.copy(hoveredCells = emptyList()) }
            return
        }

        val canPlace = gameEngine.canPlace(state, piece, origin)
        val hovered = piece.absoluteCells(origin).map { (coord, _) ->
            coord to canPlace
        }
        _uiState.update { it.copy(hoveredCells = hovered) }
    }

    fun onDragEnd() {
        val origin = lastHoveredOrigin
        onCellHover(null) // clear hover
        
        val state = currentGameState ?: return
        if (origin == null) return

        if (gameEngine.canPlace(state, state.currentPiece, origin)) {
            val result = gameEngine.placePiece(state, state.currentPiece, origin)
            updateState(result.state)
        }
    }

    fun onCellTapped(cell: HexCoord) {
        _uiState.update { it.copy(debugSelectedCell = cell) }
        
        // Tap to place
        val state = currentGameState ?: return
        if (gameEngine.canPlace(state, state.currentPiece, cell)) {
            val result = gameEngine.placePiece(state, state.currentPiece, cell)
            updateState(result.state)
        }
    }
}
