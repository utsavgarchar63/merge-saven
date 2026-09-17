package com.mergeseven.game.game.solver

import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.game.model.GameState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Off-main-thread solver with a hard time budget and result cache (AF4-03).
 */
@Singleton
class CachedMoveSolver @Inject constructor(
    private val moveSolver: MoveSolver,
    private val dispatchers: DispatcherProvider
) {
    private val mutex = Mutex()
    private var cachedKey: Long? = null
    private var cachedHint: MoveHint? = null

    suspend fun findBest(
        state: GameState,
        profile: DifficultyProfile
    ): MoveHint? = withContext(dispatchers.default) {
        val key = cacheKey(state)
        mutex.withLock {
            if (cachedKey == key) return@withContext cachedHint
        }
        val budget = HintSearchBudget(Constants.HINT_SOLVER_BUDGET_MS)
        var hint = moveSolver.findBestMove(state, profile, budget)
        if (hint == null && budget.expired()) {
            hint = moveSolver.findAnyLegalMove(state)
        }
        mutex.withLock {
            cachedKey = key
            cachedHint = hint
        }
        hint
    }

    fun invalidate() {
        cachedKey = null
        cachedHint = null
    }

    fun peekCached(state: GameState): MoveHint? =
        if (cachedKey == cacheKey(state)) cachedHint else null

    fun cacheKey(state: GameState): Long {
        var h = 17L
        h = 31 * h + state.board.occupiedCount
        h = 31 * h + state.board.fillRatio.toBits().toLong()
        for (tile in state.board.activeTiles().sortedWith(compareBy({ it.cell.q }, { it.cell.r }))) {
            h = 31 * h + tile.id
            h = 31 * h + tile.value
            h = 31 * h + tile.cell.q
            h = 31 * h + tile.cell.r
        }
        state.trayPieces.forEachIndexed { index, piece ->
            h = 31 * h + index
            if (piece == null) {
                h = 31 * h + 7
            } else {
                h = 31 * h + piece.id
                h = 31 * h + piece.rotation
                for (cell in piece.cells) {
                    h = 31 * h + cell.value
                    h = 31 * h + cell.offset.q
                    h = 31 * h + cell.offset.r
                }
            }
        }
        return h
    }
}
