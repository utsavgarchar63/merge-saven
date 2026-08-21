package com.mergeseven.game.game.model

/**
 * Actions the player can perform in the game.
 * These are dispatched from the UI to the ViewModel.
 */
sealed interface GameAction {
    /** Place the current piece at the given origin. */
    data class PlacePiece(val origin: HexCoord) : GameAction

    /** Rotate the current piece clockwise. */
    data object RotatePiece : GameAction

    /** Select a piece from the queue by index. */
    data class SelectPiece(val index: Int) : GameAction

    /** Use a booster. */
    data class UseBooster(val type: BoosterType) : GameAction

    /** Undo the last move. */
    data object Undo : GameAction

    /** Pause the game. */
    data object Pause : GameAction

    /** Resume the game. */
    data object Resume : GameAction

    /** Restart the current level. */
    data object Restart : GameAction

    /** Continue after game over (e.g., via rewarded ad). */
    data object Continue : GameAction

    /** Navigate to home. */
    data object ExitToHome : GameAction
}
