package com.mergeseven.game.data.local.snapshot

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.RngState
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.objectives.LevelObjective
import com.mergeseven.game.game.objectives.StarRating
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-disk form of [GameState].
 */
@Serializable
data class GameStateSnapshot(
    val version: Int = CURRENT_VERSION,
    val playableCells: List<HexCoord>,
    val tiles: List<Tile>,
    val trayPieces: List<TilePiece?>,
    val score: Long,
    val bestScore: Long,
    val coins: Int,
    val level: Int,
    val targetValue: Int,
    val moves: Int,
    val isGameOver: Boolean,
    val rng: RngState,
    /** Cell modifiers (AF1-12). Defaults empty so v2 saves without this field still resume. */
    val modifiers: List<CellModifierSnapshot> = emptyList(),
    /** Level objectives (AF1-13). Empty → synthesize ReachValue(targetValue) on restore. */
    val objectives: List<LevelObjective> = emptyList(),
    val collectedByValue: List<CollectedEntry> = emptyList(),
    val threeStarMoveCap: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
    val twoStarMoveCap: Int = StarRating.DEFAULT_TWO_STAR_MOVES,
    /** AF2 mode fields — defaults keep v2 JSON loadable. */
    val modeId: String = ModeIds.CAMPAIGN,
    val timeRemainingMs: Long = 0L,
    val spawnTier: Int = 0,
    val forceTriplePieces: Boolean = false,
    val sessionDateKey: String = "",
    val continuesUsed: Int = 0,
    val rewardedContinuesUsed: Int = 0,
    val undosUsedThisRun: Int = 0,
    val undoBlockedUntilMove: Boolean = false,
    val timeFrozenMs: Long = 0L,
    val spawnValueBoost: List<CollectedEntry> = emptyList(),
    val previous: GameStateSnapshot? = null
) {
    companion object {
        const val CURRENT_VERSION = 3
        /** Oldest snapshot entity schema still accepted on load. */
        const val MIN_SUPPORTED_VERSION = 2

        val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }
    }
}

@Serializable
data class CellModifierSnapshot(
    val cell: HexCoord,
    val type: com.mergeseven.game.game.model.CellModifierType,
    val scoreBonus: Float = 1.5f
)

@Serializable
data class CollectedEntry(
    val value: Int,
    val count: Int
)

fun GameState.toSnapshot(maxUndoDepth: Int = Constants.MAX_UNDO_HISTORY): GameStateSnapshot =
    GameStateSnapshot(
        version = GameStateSnapshot.CURRENT_VERSION,
        playableCells = board.playableCells.toList(),
        tiles = board.activeTiles(),
        trayPieces = trayPieces,
        score = score,
        bestScore = bestScore,
        coins = coins,
        level = level,
        targetValue = targetValue,
        moves = moves,
        isGameOver = isGameOver,
        rng = rng,
        modifiers = board.cellModifiers.map { (cell, modifier) ->
            CellModifierSnapshot(
                cell = cell,
                type = modifier.type,
                scoreBonus = modifier.scoreBonus
            )
        },
        objectives = objectives,
        collectedByValue = collectedByValue.map { (value, count) ->
            CollectedEntry(value = value, count = count)
        },
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
        spawnValueBoost = spawnValueBoost.map { (value, count) ->
            CollectedEntry(value = value, count = count)
        },
        previous = if (maxUndoDepth > 0) previousState?.toSnapshot(maxUndoDepth - 1) else null
    )

fun GameStateSnapshot.toGameState(): GameState {
    val emptyCells: Map<HexCoord, Tile?> = playableCells.associateWith { null }
    val occupied: Map<HexCoord, Tile?> = tiles.associateBy { it.cell }
    val modifierMap = modifiers.associate { snap ->
        snap.cell to com.mergeseven.game.game.model.CellModifier(
            type = snap.type,
            scoreBonus = snap.scoreBonus
        )
    }
    val collectMap = collectedByValue.associate { it.value to it.count }
    val resolvedObjectives = if (modeId == ModeIds.CAMPAIGN || modeId == ModeIds.DAILY) {
        objectives.ifEmpty {
            listOf(LevelObjective.ReachValue(id = "reach_target", value = targetValue))
        }
    } else {
        objectives
    }

    return GameState(
        board = BoardState(
            cells = emptyCells + occupied,
            playableCells = playableCells.toSet(),
            cellModifiers = modifierMap
        ),
        trayPieces = trayPieces,
        score = score,
        bestScore = bestScore,
        coins = coins,
        level = level,
        targetValue = targetValue,
        moves = moves,
        isPaused = false,
        isBusy = false,
        isGameOver = isGameOver,
        rng = rng,
        objectives = resolvedObjectives,
        collectedByValue = collectMap,
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
        spawnValueBoost = spawnValueBoost.associate { it.value to it.count },
        previousState = previous?.toGameState()
    )
}
