package com.mergeseven.game.game.engine

import com.mergeseven.game.game.levels.LevelBoardFactory
import com.mergeseven.game.game.model.*
import kotlin.random.Random

/**
 * Core game engine interface.
 * See Master Plan Section 42.
 *
 * Keeps UI ignorant of implementation details.
 * All game logic goes through this interface.
 */
interface GameEngine {
    fun placePiece(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord,
        slotIndex: Int = 0
    ): GameResult

    fun rotatePiece(piece: TilePiece): TilePiece
    fun rotateTrayPiece(state: GameState, slotIndex: Int): GameState
    fun shuffleTray(state: GameState): GameState
    fun undo(state: GameState): GameState
    fun removeTile(state: GameState, coord: HexCoord): GameState

    fun continueAfterGameOver(
        state: GameState,
        clearCount: Int = 5,
        rewarded: Boolean = false
    ): GameResult
    fun hammerTile(state: GameState, coord: HexCoord): GameResult
    fun valueUpTile(state: GameState, coord: HexCoord): GameResult
    fun magnetPull(state: GameState, coord: HexCoord): GameResult
    fun timeFreeze(state: GameState, freezeMs: Long): GameResult

    fun canPlace(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean

    fun isGameOver(state: GameState): Boolean

    /**
     * @param seed fixes the run's piece sequence. Defaults to a fresh random value; pass a derived
     *   one (a date, a puzzle id) when every player must get the same board.
     */
    fun createInitialState(
        level: Int = 1,
        bestScore: Long = 0,
        seed: Long = Random.nextLong()
    ): GameState
}

class GameEngineImpl(
    private val boardEngine: BoardEngine,
    private val mergeEngine: MergeEngine,
    private val placementEngine: PlacementEngine,
    private val spawnEngine: SpawnEngine,
    private val scoreEngine: ScoreEngine,
    private val gameOverEngine: GameOverEngine,
    private val chainReactionEngine: ChainReactionEngine,
    private val levelBoardFactory: LevelBoardFactory? = null
) : GameEngine {

    override fun placePiece(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord,
        slotIndex: Int
    ): GameResult {
        if (!canPlace(state, piece, origin)) {
            return GameResult(
                state = state,
                events = listOf(
                    GameEvent.InvalidPlacement(origin, "Cannot place piece here")
                )
            )
        }

        val events = mutableListOf<GameEvent>()
        val random = GameRandom(state.rng)

        val placedTiles = placementEngine.place(state.board, piece, origin, random)
        var newBoard = placedTiles.fold(state.board) { board, tile ->
            board.withTile(tile)
        }
        events.add(GameEvent.TilePlaced(piece, origin, placedTiles))

        val chainResult = chainReactionEngine.resolveChains(newBoard, random, placedTiles)
        newBoard = chainResult.finalBoard
        events.addAll(chainResult.events)

        // AF1-12: at most one spawn-vent tile per move.
        val ventSpawn = maybeSpawnOnVent(newBoard, state.level, random)
        if (ventSpawn != null) {
            newBoard = newBoard.withTile(ventSpawn)
            events.add(GameEvent.VentSpawned(ventSpawn))
        }

        val scoreEarned = chainResult.events
            .filterIsInstance<GameEvent.MergeCompleted>()
            .sumOf { it.scoreEarned }

        val collectTargets = com.mergeseven.game.game.objectives.ObjectiveEvaluator
            .effectiveObjectives(state)
            .filterIsInstance<com.mergeseven.game.game.objectives.LevelObjective.CollectValue>()
            .map { it.tileValue }
            .toSet()
        val collectUpdates = if (collectTargets.isEmpty()) {
            state.collectedByValue
        } else {
            chainResult.events
                .filterIsInstance<GameEvent.MergeCompleted>()
                .map { it.resultTile.value }
                .filter { it in collectTargets }
                .fold(state.collectedByValue) { acc, value ->
                    acc + (value to (acc[value] ?: 0) + 1)
                }
        }

        val newTray = state.trayPieces.toMutableList()
        val targetSlot = if (slotIndex in newTray.indices) slotIndex else 0
        val hints = SpawnHints(
            tier = state.spawnTier,
            forceTriple = state.forceTriplePieces,
            boardValueBoost = state.spawnValueBoost,
            baseWeights = state.spawnBaseWeights.takeIf { it.isNotEmpty() }
        )
        newTray[targetSlot] = spawnEngine.generatePiece(state.level, random, hints)

        val newState = state.copy(
            board = newBoard,
            trayPieces = newTray,
            score = state.score + scoreEarned,
            bestScore = maxOf(state.bestScore, state.score + scoreEarned),
            moves = state.moves + 1,
            rng = random.snapshot(),
            collectedByValue = collectUpdates,
            undoBlockedUntilMove = false,
            previousState = state
        )

        // Win/lose is decided by GameMode.afterMove (AF2-01).
        return GameResult(state = newState, events = events)
    }

    private fun maybeSpawnOnVent(
        board: BoardState,
        level: Int,
        random: GameRandom
    ): Tile? {
        val emptyVents = board.cellModifiers
            .filter { (coord, modifier) ->
                modifier.type == CellModifierType.SPAWN_VENT &&
                    board.isPlayable(coord) &&
                    board.isEmpty(coord) &&
                    !board.isLocked(coord)
            }
            .keys
            .sortedWith(compareBy({ it.q }, { it.r }))

        if (emptyVents.isEmpty()) return null
        val cell = emptyVents[random.nextInt(emptyVents.size)]
        return Tile(
            id = random.nextId(),
            value = spawnEngine.generateValue(level, random),
            cell = cell,
            trait = TileTrait.NORMAL
        )
    }

    override fun rotatePiece(piece: TilePiece): TilePiece {
        return piece.rotateClockwise()
    }

    override fun rotateTrayPiece(state: GameState, slotIndex: Int): GameState {
        val tray = state.trayPieces.toMutableList()
        if (slotIndex in tray.indices) {
            val piece = tray[slotIndex]
            if (piece != null) {
                tray[slotIndex] = piece.rotateClockwise()
            }
        }
        return state.copy(trayPieces = tray)
    }

    override fun shuffleTray(state: GameState): GameState {
        val random = GameRandom(state.rng)
        val hints = SpawnHints(
            tier = state.spawnTier,
            forceTriple = state.forceTriplePieces,
            boardValueBoost = state.spawnValueBoost,
            baseWeights = state.spawnBaseWeights.takeIf { it.isNotEmpty() }
        )
        val newTray = List(3) { spawnEngine.generatePiece(state.level, random, hints) }
        return state.copy(trayPieces = newTray, rng = random.snapshot())
    }

    override fun undo(state: GameState): GameState {
        return state.previousState ?: state
    }

    override fun removeTile(state: GameState, coord: HexCoord): GameState {
        val tile = state.board.tileAt(coord) ?: return state
        if (!com.mergeseven.game.game.engine.traits.TraitInteractions.canRemoveWithBooster(tile)) {
            return state
        }
        val newBoard = state.board.withoutTile(coord)
        return state.copy(board = newBoard)
    }

    override fun continueAfterGameOver(
        state: GameState,
        clearCount: Int,
        rewarded: Boolean
    ): GameResult {
        if (!state.isGameOver) {
            return GameResult(state, emptyList())
        }
        val removable = state.board.activeTiles()
            .filter { com.mergeseven.game.game.engine.traits.TraitInteractions.canRemoveWithBooster(it) }
            .sortedWith(compareBy({ it.value }, { it.cell.q }, { it.cell.r }))
            .take(clearCount.coerceAtLeast(0))
        var board = state.board
        removable.forEach { board = board.withoutTile(it.cell) }
        val newState = state.copy(
            board = board,
            isGameOver = false,
            previousState = null,
            continuesUsed = if (rewarded) state.continuesUsed else state.continuesUsed + 1,
            rewardedContinuesUsed = if (rewarded) {
                state.rewardedContinuesUsed + 1
            } else {
                state.rewardedContinuesUsed
            },
            undoBlockedUntilMove = true
        )
        return GameResult(
            state = newState,
            events = listOf(GameEvent.BoosterActivated(BoosterType.CONTINUE))
        )
    }

    override fun hammerTile(state: GameState, coord: HexCoord): GameResult {
        val next = removeTile(state, coord)
        if (next === state || next.board == state.board) {
            return GameResult(state, emptyList())
        }
        return GameResult(
            state = next.copy(previousState = state),
            events = listOf(GameEvent.BoosterActivated(BoosterType.HAMMER))
        )
    }

    override fun valueUpTile(state: GameState, coord: HexCoord): GameResult {
        val tile = state.board.tileAt(coord) ?: return GameResult(state, emptyList())
        if (!com.mergeseven.game.game.engine.traits.TraitInteractions.canRemoveWithBooster(tile)) {
            return GameResult(state, emptyList())
        }
        if (tile.value >= com.mergeseven.game.core.Constants.VALUE_UP_CAP) {
            return GameResult(state, emptyList())
        }
        val upgraded = tile.copy(value = (tile.value * 2).coerceAtMost(com.mergeseven.game.core.Constants.VALUE_UP_CAP))
        val newState = state.copy(
            board = state.board.withTile(upgraded),
            previousState = state
        )
        return GameResult(
            state = newState,
            events = listOf(GameEvent.BoosterActivated(BoosterType.VALUE_UP))
        )
    }

    override fun magnetPull(state: GameState, coord: HexCoord): GameResult {
        val anchor = state.board.tileAt(coord) ?: return GameResult(state, emptyList())
        val value = anchor.value
        val others = state.board.activeTiles()
            .filter { it.cell != coord && it.value == value }
            .sortedWith(compareBy({ it.cell.q }, { it.cell.r }))
        if (others.isEmpty()) {
            return GameResult(state, listOf(GameEvent.BoosterActivated(BoosterType.MAGNET)))
        }
        var board = state.board
        for (tile in others) {
            val step = stepToward(tile.cell, coord) ?: continue
            if (!board.isPlayable(step) || !board.isEmpty(step) || board.isLocked(step)) continue
            board = board.withoutTile(tile.cell).withTile(tile.copy(cell = step))
        }
        val newState = state.copy(board = board, previousState = state)
        return GameResult(
            state = newState,
            events = listOf(GameEvent.BoosterActivated(BoosterType.MAGNET))
        )
    }

    override fun timeFreeze(state: GameState, freezeMs: Long): GameResult {
        val newState = state.copy(
            timeFrozenMs = state.timeFrozenMs + freezeMs.coerceAtLeast(0L),
            previousState = state
        )
        return GameResult(
            state = newState,
            events = listOf(GameEvent.BoosterActivated(BoosterType.TIME_FREEZE))
        )
    }

    /** One hex step from [from] toward [to], or null if already adjacent/same. */
    private fun stepToward(from: HexCoord, to: HexCoord): HexCoord? {
        if (from == to) return null
        val dq = (to.q - from.q).coerceIn(-1, 1)
        val dr = (to.r - from.r).coerceIn(-1, 1)
        // Prefer a real neighbor that reduces distance.
        val candidates = from.neighbors()
            .filter { n ->
                val before = hexDistance(from, to)
                val after = hexDistance(n, to)
                after < before
            }
            .sortedWith(compareBy({ it.q }, { it.r }))
        return candidates.firstOrNull()
            ?: from.copy(q = from.q + dq, r = from.r + dr).takeIf { it in from.neighbors() }
    }

    private fun hexDistance(a: HexCoord, b: HexCoord): Int {
        val dq = a.q - b.q
        val dr = a.r - b.r
        val ds = a.s - b.s
        return (kotlin.math.abs(dq) + kotlin.math.abs(dr) + kotlin.math.abs(ds)) / 2
    }

    override fun canPlace(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean {
        return placementEngine.canPlace(state.board, piece, origin)
    }

    override fun isGameOver(state: GameState): Boolean {
        return gameOverEngine.isGameOver(state)
    }

    override fun createInitialState(level: Int, bestScore: Long, seed: Long): GameState {
        val random = GameRandom(RngState.fromSeed(seed))
        val prepared = levelBoardFactory?.prepare(level, random)
        val board = prepared?.board ?: boardEngine.createBoard()
        val target = prepared?.targetValue
            ?: com.mergeseven.game.game.rules.LevelPool.getLevel(level).target.toInt()
        val objectives = prepared?.objectives
            ?: listOf(
                com.mergeseven.game.game.objectives.LevelObjective.ReachValue(
                    id = "reach_target",
                    value = target
                )
            )
        val pieces = List(3) { spawnEngine.generatePiece(level, random) }
        return GameState.initial(
            board = board,
            trayPieces = pieces,
            level = level,
            targetValue = target,
            bestScore = bestScore,
            rng = random.snapshot(),
            objectives = objectives,
            threeStarMoveCap = prepared?.threeStarMoveCap
                ?: com.mergeseven.game.game.objectives.StarRating.DEFAULT_THREE_STAR_MOVES,
            twoStarMoveCap = prepared?.twoStarMoveCap
                ?: com.mergeseven.game.game.objectives.StarRating.DEFAULT_TWO_STAR_MOVES
        )
    }
}
