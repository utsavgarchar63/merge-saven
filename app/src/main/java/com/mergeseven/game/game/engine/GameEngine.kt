package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.*

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

    fun canPlace(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean

    fun isGameOver(state: GameState): Boolean

    fun createInitialState(
        level: Int = 1,
        bestScore: Long = 0
    ): GameState
}

class GameEngineImpl(
    private val boardEngine: BoardEngine,
    private val mergeEngine: MergeEngine,
    private val placementEngine: PlacementEngine,
    private val spawnEngine: SpawnEngine,
    private val scoreEngine: ScoreEngine,
    private val gameOverEngine: GameOverEngine,
    private val chainReactionEngine: ChainReactionEngine
) : GameEngine {

    override fun placePiece(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord,
        slotIndex: Int
    ): GameResult {
        // Validate placement
        if (!canPlace(state, piece, origin)) {
            return GameResult(
                state = state,
                events = listOf(
                    GameEvent.InvalidPlacement(origin, "Cannot place piece here")
                )
            )
        }

        val events = mutableListOf<GameEvent>()

        // 1. Place the piece on the board
        val placedTiles = placementEngine.place(state.board, piece, origin)
        var newBoard = placedTiles.fold(state.board) { board, tile ->
            board.withTile(tile)
        }
        events.add(GameEvent.TilePlaced(piece, origin, placedTiles))

        // 2. Run merge chain reactions
        val chainResult = chainReactionEngine.resolveChains(newBoard, placedTiles)
        newBoard = chainResult.finalBoard
        events.addAll(chainResult.events)

        // 3. Calculate score
        val scoreEarned = chainResult.events
            .filterIsInstance<GameEvent.MergeCompleted>()
            .sumOf { it.scoreEarned }

        // 4. Update tray: Replace used piece at slotIndex with a new spawned piece
        val newTray = state.trayPieces.toMutableList()
        val targetSlot = if (slotIndex in newTray.indices) slotIndex else 0
        newTray[targetSlot] = spawnEngine.generatePiece(state.level)

        // 5. Build new state (saving previousState for Undo)
        var newState = state.copy(
            board = newBoard,
            trayPieces = newTray,
            score = state.score + scoreEarned,
            bestScore = maxOf(state.bestScore, state.score + scoreEarned),
            moves = state.moves + 1,
            previousState = state
        )

        // 6. Check level completion
        val maxTile = newBoard.activeTiles().maxOfOrNull { it.value } ?: 0
        if (maxTile >= state.targetValue) {
            events.add(GameEvent.LevelCompleted(state.level, newState.score, maxTile))
        }

        // 7. Check game over
        if (isGameOver(newState)) {
            newState = newState.copy(isGameOver = true)
            events.add(GameEvent.GameOver)
        }

        return GameResult(state = newState, events = events)
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
        val newTray = List(3) { spawnEngine.generatePiece(state.level) }
        return state.copy(trayPieces = newTray)
    }

    override fun undo(state: GameState): GameState {
        return state.previousState ?: state
    }

    override fun removeTile(state: GameState, coord: HexCoord): GameState {
        val newBoard = state.board.withoutTile(coord)
        return state.copy(board = newBoard)
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

    override fun createInitialState(level: Int, bestScore: Long): GameState {
        val board = boardEngine.createBoard()
        val pieces = List(3) { spawnEngine.generatePiece(level) }
        val levelRule = com.mergeseven.game.game.rules.LevelPool.getLevel(level)
        return GameState.initial(
            board = board,
            trayPieces = pieces,
            level = level,
            targetValue = levelRule.target.toInt(),
            bestScore = bestScore
        )
    }
}
