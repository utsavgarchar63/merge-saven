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

    /**
     * Place a piece on the board at the given origin.
     * Returns a GameResult containing the new state and events
     * (placement, merges, chain reactions, score, coins, etc.)
     */
    fun placePiece(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): GameResult

    /**
     * Rotate a piece clockwise by 60°.
     */
    fun rotatePiece(piece: TilePiece): TilePiece

    /**
     * Check if a piece can be placed at the given origin.
     */
    fun canPlace(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean

    /**
     * Check if the game is over (no valid placements exist).
     */
    fun isGameOver(state: GameState): Boolean

    /**
     * Create a new initial game state for the given level.
     */
    fun createInitialState(
        level: Int = 1,
        bestScore: Long = 0
    ): GameState
}

/**
 * Default implementation of the game engine.
 * Delegates to specialized sub-engines for separation of concerns.
 */
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
        origin: HexCoord
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

        // 4. Generate next piece and advance queue
        val nextPieces = state.nextPieces.toMutableList()
        val newCurrentPiece = if (nextPieces.isNotEmpty()) {
            nextPieces.removeAt(0)
        } else {
            spawnEngine.generatePiece(state.level)
        }
        nextPieces.add(spawnEngine.generatePiece(state.level))

        // 5. Build new state
        var newState = state.copy(
            board = newBoard,
            currentPiece = newCurrentPiece,
            nextPieces = nextPieces,
            score = state.score + scoreEarned,
            bestScore = maxOf(state.bestScore, state.score + scoreEarned),
            moves = state.moves + 1
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
        return GameState.initial(
            board = board,
            currentPiece = pieces[0],
            nextPieces = pieces.drop(1),
            level = level,
            bestScore = bestScore
        )
    }
}
