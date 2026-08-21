package com.mergeseven.game.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
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
    val tiles: Map<HexCoord, Pair<Int, Color>> = emptyMap()
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
                tiles = tilesUi
            )
        }
    }
}
