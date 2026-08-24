package com.mergeseven.game.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.data.repository.LevelRepository
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
    val targetValue: Int = 16,
    val boardRadius: Int = Constants.DEFAULT_BOARD_RADIUS,
    val boardCells: Set<HexCoord> = emptySet(),
    val tiles: Map<HexCoord, Pair<Int, Color>> = emptyMap(),
    val trayPieces: List<TilePiece?> = listOf(null, null, null),
    val selectedSlotIndex: Int = 0,
    val isGameOver: Boolean = false,
    val isLevelComplete: Boolean = false,
    val starsEarned: Int = 3,
    val canUndo: Boolean = false,
    val hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList() // coord to isValid
) {
    val currentPiece: TilePiece?
        get() = trayPieces.getOrNull(selectedSlotIndex) ?: trayPieces.firstOrNull { it != null }

    val nextPieces: List<TilePiece>
        get() = trayPieces.filterNotNull()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameEngine: GameEngine,
    private val audioManager: AudioManager,
    private val levelRepository: LevelRepository
) : ViewModel() {

    private val selectedLevelId: Int = savedStateHandle.get<Int>("levelId") ?: 1

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentGameState: GameState? = null

    init {
        startNewGame(selectedLevelId)
        audioManager.startMusic()
    }

    fun startNewGame(levelId: Int = selectedLevelId) {
        val initialState = gameEngine.createInitialState(
            level = levelId,
            bestScore = 0
        )
        updateState(initialState)
        audioManager.startMusic()
    }

    fun onNextLevel() {
        val nextLevel = _uiState.value.level + 1
        startNewGame(nextLevel)
    }

    fun onPause() {
        audioManager.pauseMusic()
    }

    fun onResume() {
        audioManager.startMusic()
    }

    private fun updateState(gameState: GameState) {
        currentGameState = gameState

        val tilesUi = gameState.board.activeTiles().associate { tile ->
            tile.cell to Pair(tile.value, GameColors.tileColor(tile.value))
        }

        val availableSlot = if (gameState.trayPieces.getOrNull(_uiState.value.selectedSlotIndex) != null) {
            _uiState.value.selectedSlotIndex
        } else {
            gameState.trayPieces.indexOfFirst { it != null }.coerceAtLeast(0)
        }

        // Check level completion
        val maxTileValue = gameState.board.activeTiles().maxOfOrNull { it.value } ?: 0
        val isComplete = maxTileValue >= gameState.targetValue
        if (isComplete) {
            levelRepository.completeLevel(gameState.level, gameState.score, 3)
            audioManager.playSoundCombo()
        }

        _uiState.update { currentState ->
            currentState.copy(
                level = gameState.level,
                score = gameState.score,
                coins = gameState.coins,
                targetValue = gameState.targetValue,
                boardCells = gameState.board.playableCells,
                tiles = tilesUi,
                trayPieces = gameState.trayPieces,
                selectedSlotIndex = availableSlot,
                isGameOver = gameState.isGameOver,
                isLevelComplete = isComplete,
                starsEarned = 3,
                canUndo = gameState.previousState != null
            )
        }
    }

    fun onSelectTraySlot(index: Int) {
        val state = currentGameState ?: return
        if (index !in state.trayPieces.indices) return
        _uiState.update { it.copy(selectedSlotIndex = index) }
    }

    fun onRotateTraySlot(index: Int) {
        val state = currentGameState ?: return
        val newState = gameEngine.rotateTrayPiece(state, index)
        updateState(newState)
    }

    private var lastHoveredOrigin: HexCoord? = null
    private var lastHoveredSlotIndex: Int = 0

    fun onCellHover(origin: HexCoord?, slotIndex: Int = _uiState.value.selectedSlotIndex) {
        val state = currentGameState ?: return
        val piece = state.trayPieces.getOrNull(slotIndex)
        lastHoveredOrigin = origin
        lastHoveredSlotIndex = slotIndex

        if (origin == null || piece == null) {
            _uiState.update { it.copy(hoveredCells = emptyList()) }
            return
        }

        val canPlace = gameEngine.canPlace(state, piece, origin)
        val hovered = piece.absoluteCells(origin).map { (coord, _) ->
            coord to canPlace
        }
        _uiState.update { it.copy(hoveredCells = hovered) }
    }

    fun onDropOnCell(origin: HexCoord, slotIndex: Int = _uiState.value.selectedSlotIndex) {
        onCellHover(null, slotIndex)
        val state = currentGameState ?: return
        val piece = state.trayPieces.getOrNull(slotIndex) ?: return

        if (gameEngine.canPlace(state, piece, origin)) {
            val result = gameEngine.placePiece(state, piece, origin, slotIndex)
            handleResultAudio(result)
            updateState(result.state)
        }
    }

    fun onCellTapped(cell: HexCoord) {
        val state = currentGameState ?: return
        val slotIndex = _uiState.value.selectedSlotIndex
        val piece = state.trayPieces.getOrNull(slotIndex) ?: return

        if (gameEngine.canPlace(state, piece, cell)) {
            val result = gameEngine.placePiece(state, piece, cell, slotIndex)
            handleResultAudio(result)
            updateState(result.state)
        }
    }

    private fun handleResultAudio(result: com.mergeseven.game.game.model.GameResult) {
        val hasMerge = result.events.any { it is com.mergeseven.game.game.model.GameEvent.MergeCompleted }
        val hasCombo = result.events.any { it is com.mergeseven.game.game.model.GameEvent.ChainCompleted }

        audioManager.playSoundPlace()
        if (hasCombo) {
            audioManager.playSoundCombo()
        } else if (hasMerge) {
            audioManager.playSoundMerge()
        }
    }

    // Boosters
    fun onBoosterUndo() {
        val state = currentGameState ?: return
        val newState = gameEngine.undo(state)
        updateState(newState)
    }

    fun onBoosterShuffle() {
        val state = currentGameState ?: return
        val newState = gameEngine.shuffleTray(state)
        updateState(newState)
    }

    fun onBoosterRotate() {
        val slotIndex = _uiState.value.selectedSlotIndex
        onRotateTraySlot(slotIndex)
    }

    fun onBoosterRemoveTile(cell: HexCoord) {
        val state = currentGameState ?: return
        val newState = gameEngine.removeTile(state, cell)
        updateState(newState)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.pauseMusic()
    }
}
