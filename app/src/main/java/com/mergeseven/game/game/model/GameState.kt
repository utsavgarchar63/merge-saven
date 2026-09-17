package com.mergeseven.game.game.model

import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.objectives.LevelObjective
import com.mergeseven.game.game.objectives.StarRating
import kotlinx.serialization.Serializable

/**
 * The complete game state.
 * See Master Plan Section 8.5 / AF1-13–15 / AF2.
 */
@Serializable
data class GameState(
    val board: BoardState,
    val trayPieces: List<TilePiece?>,
    val score: Long,
    val bestScore: Long,
    val coins: Int,
    val level: Int,
    val targetValue: Int,
    val moves: Int,
    val isPaused: Boolean,
    val isGameOver: Boolean,
    val isBusy: Boolean,
    /**
     * Where this run is in its random sequence. Travels with the state so that undo rewinds the
     * RNG too, and so a save resumes the same piece stream rather than starting a new one.
     */
    val rng: RngState = RngState.fromSeed(0L),
    /** Level objectives; empty means synthesize ReachValue(targetValue). */
    val objectives: List<LevelObjective> = emptyList(),
    /** Merge-created tiles counted toward CollectValue objectives. */
    val collectedByValue: Map<Int, Int> = emptyMap(),
    val threeStarMoveCap: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
    val twoStarMoveCap: Int = StarRating.DEFAULT_TWO_STAR_MOVES,
    /** Active [com.mergeseven.game.game.modes.GameMode] id (AF2). */
    val modeId: String = ModeIds.CAMPAIGN,
    /** Time Attack countdown (AF2-04). */
    val timeRemainingMs: Long = 0L,
    /** Endless spawn escalation tier (AF2-03). */
    val spawnTier: Int = 0,
    /** Weekly force-triple pieces (AF2-07). */
    val forceTriplePieces: Boolean = false,
    /** Daily/weekly date or week key for stale-save detection. */
    val sessionDateKey: String = "",
    /** Coin continues used this run (AF3-03). */
    val continuesUsed: Int = 0,
    /** Rewarded continues used this run. */
    val rewardedContinuesUsed: Int = 0,
    /** Successful undos this run (AF3-09). */
    val undosUsedThisRun: Int = 0,
    /** After CONTINUE until the next place (AF3-09). */
    val undoBlockedUntilMove: Boolean = false,
    /** Time Attack freeze remaining (AF3-04). */
    val timeFrozenMs: Long = 0L,
    /** AF4-07 adaptive spawn boost keyed by tile value. */
    val spawnValueBoost: Map<Int, Int> = emptyMap(),
    /** AF6-04 Remote Config base spawn weights; empty = use Constants.SPAWN_WEIGHTS. */
    val spawnBaseWeights: Map<Int, Int> = emptyMap(),
    val previousState: GameState? = null
) {
    val currentPiece: TilePiece?
        get() = trayPieces.firstOrNull { it != null }

    val nextPieces: List<TilePiece>
        get() = trayPieces.filterNotNull().drop(1)

    companion object {
        fun initial(
            board: BoardState,
            trayPieces: List<TilePiece>,
            level: Int = 1,
            targetValue: Int = 16,
            initialCoins: Int = 100,
            bestScore: Long = 0,
            rng: RngState = RngState.fromSeed(0L),
            objectives: List<LevelObjective> = emptyList(),
            threeStarMoveCap: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
            twoStarMoveCap: Int = StarRating.DEFAULT_TWO_STAR_MOVES,
            modeId: String = ModeIds.CAMPAIGN,
            timeRemainingMs: Long = 0L,
            spawnTier: Int = 0,
            forceTriplePieces: Boolean = false,
            sessionDateKey: String = "",
            continuesUsed: Int = 0,
            rewardedContinuesUsed: Int = 0,
            undosUsedThisRun: Int = 0,
            undoBlockedUntilMove: Boolean = false,
            timeFrozenMs: Long = 0L,
            spawnValueBoost: Map<Int, Int> = emptyMap()
        ): GameState = GameState(
            board = board,
            trayPieces = trayPieces.take(3),
            score = 0L,
            bestScore = bestScore,
            coins = initialCoins,
            level = level,
            targetValue = targetValue,
            moves = 0,
            isPaused = false,
            isGameOver = false,
            isBusy = false,
            rng = rng,
            objectives = objectives,
            collectedByValue = emptyMap(),
            threeStarMoveCap = threeStarMoveCap,
            twoStarMoveCap = twoStarMoveCap,
            modeId = modeId,
            timeRemainingMs = timeRemainingMs,
            spawnTier = spawnTier,
            forceTriplePieces = forceTriplePieces,
            sessionDateKey = sessionDateKey,
            continuesUsed = continuesUsed,
            rewardedContinuesUsed = rewardedContinuesUsed,
            undosUsedThisRun = undosUsedThisRun,
            undoBlockedUntilMove = undoBlockedUntilMove,
            timeFrozenMs = timeFrozenMs,
            spawnValueBoost = spawnValueBoost,
            previousState = null
        )
    }
}
