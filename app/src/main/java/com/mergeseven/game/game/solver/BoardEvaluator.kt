package com.mergeseven.game.game.solver

import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.CellModifierType
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heuristic board quality score — higher is better (AF4-01).
 */
@Singleton
class BoardEvaluator @Inject constructor() {

    fun score(
        state: GameState,
        weights: EvaluatorWeights = EvaluatorWeights()
    ): Double = scoreBoard(state.board, state.targetValue, state.score, weights)

    fun scoreBoard(
        board: BoardState,
        targetValue: Int = 0,
        score: Long = 0L,
        weights: EvaluatorWeights = EvaluatorWeights()
    ): Double {
        val open = board.emptyCells().size
        val fill = board.fillRatio.toDouble()
        val clusters = sameValueNeighborPairs(board)
        val maxTile = board.activeTiles().maxOfOrNull { it.value } ?: 0
        val locked = board.cellModifiers.count { it.value.type == CellModifierType.LOCKED }
        val targetProgress = when {
            targetValue > 0 -> score.toDouble().coerceAtMost(targetValue.toDouble())
            else -> score.toDouble() * 0.001
        }

        return open * weights.openCells -
            fill * weights.fillPenalty +
            clusters * weights.clusterPair +
            maxTile * weights.maxTile +
            targetProgress * weights.targetProgress -
            locked * weights.lockedPenalty
    }

    private fun sameValueNeighborPairs(board: BoardState): Int {
        var pairs = 0
        val seen = HashSet<Pair<HexCoord, HexCoord>>()
        for (tile in board.activeTiles()) {
            for (n in tile.cell.neighbors()) {
                val other = board.tileAt(n) ?: continue
                if (other.value != tile.value) continue
                val edge = if (
                    tile.cell.q < n.q || (tile.cell.q == n.q && tile.cell.r < n.r)
                ) {
                    tile.cell to n
                } else {
                    n to tile.cell
                }
                if (seen.add(edge)) pairs++
            }
        }
        return pairs
    }
}
