package com.mergeseven.game.game.model

/**
 * Structured result returned by the game engine after processing an action.
 * See Master Plan Section 43.
 *
 * Cleanly separates game rules from animation/UI concerns.
 * The ViewModel uses the events list to drive animations, sound, and haptics.
 */
data class GameResult(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * Events emitted by the game engine to describe what happened.
 * See Master Plan Section 43.
 *
 * The UI/animation layer consumes these to trigger visual/audio feedback.
 */
sealed interface GameEvent {

    /** A tile piece was successfully placed on the board. */
    data class TilePlaced(
        val piece: TilePiece,
        val origin: HexCoord,
        val placedTiles: List<Tile>
    ) : GameEvent

    /** A merge animation should begin. */
    data class MergeStarted(
        val sourceTiles: List<Tile>,
        val destinationCoord: HexCoord,
        val mergedValue: Int
    ) : GameEvent

    /** A merge has been resolved — the new tile exists on the board. */
    data class MergeCompleted(
        val resultTile: Tile,
        val mergedCount: Int,
        val scoreEarned: Long
    ) : GameEvent

    /** A chain reaction completed (multiple sequential merges). */
    data class ChainCompleted(
        val chainLength: Int,
        val totalScoreEarned: Long
    ) : GameEvent

    /** Coins were earned. */
    data class CoinsEarned(
        val amount: Int,
        val reason: String
    ) : GameEvent

    /** A level was completed. */
    data class LevelCompleted(
        val level: Int,
        val score: Long,
        val maxTileValue: Int
    ) : GameEvent

    /** The game is over — no valid placements remain. */
    data object GameOver : GameEvent

    /** A booster was activated. */
    data class BoosterActivated(
        val type: BoosterType
    ) : GameEvent

    /** An undo was performed. */
    data object UndoPerformed : GameEvent

    /** Invalid placement attempted. */
    data class InvalidPlacement(
        val origin: HexCoord,
        val reason: String
    ) : GameEvent
}
