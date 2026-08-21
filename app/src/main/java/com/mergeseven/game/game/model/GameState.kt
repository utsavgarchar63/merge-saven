package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * The complete game state.
 * See Master Plan Section 8.5.
 *
 * This is the single authoritative representation of the current game.
 * The ViewModel exposes this as StateFlow for the UI to observe.
 */
@Serializable
data class GameState(
    val board: BoardState,
    val currentPiece: TilePiece,
    val nextPieces: List<TilePiece>,
    val score: Long,
    val bestScore: Long,
    val coins: Int,
    val level: Int,
    val targetValue: Int,
    val moves: Int,
    val isPaused: Boolean,
    val isGameOver: Boolean,
    val isBusy: Boolean
) {
    companion object {
        /**
         * Creates a default initial game state with an empty board.
         * Board creation is delegated to BoardEngine.
         */
        fun initial(
            board: BoardState,
            currentPiece: TilePiece,
            nextPieces: List<TilePiece>,
            level: Int = 1,
            targetValue: Int = 16,
            initialCoins: Int = 100,
            bestScore: Long = 0
        ): GameState = GameState(
            board = board,
            currentPiece = currentPiece,
            nextPieces = nextPieces,
            score = 0L,
            bestScore = bestScore,
            coins = initialCoins,
            level = level,
            targetValue = targetValue,
            moves = 0,
            isPaused = false,
            isGameOver = false,
            isBusy = false
        )
    }
}
