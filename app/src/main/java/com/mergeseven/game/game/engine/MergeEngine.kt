package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.*

/**
 * Detects and resolves tile merges on the board.
 * See Master Plan Sections 10-11 (Merge Rule, Merge Resolution), Phase 8.
 *
 * Uses BFS to find connected groups of same-value tiles.
 * Groups of MIN_MERGE_COUNT or more are merged into the next value.
 */
class MergeEngine(
    private val scoreEngine: ScoreEngine
) {

    /**
     * Find all mergeable groups on the board.
     * A group is a set of connected tiles with the same value
     * that has at least MIN_MERGE_COUNT members.
     *
     * @return List of groups, each containing the tiles in the group.
     */
    fun findMergeableGroups(board: BoardState): List<List<Tile>> {
        val visited = mutableSetOf<HexCoord>()
        val groups = mutableListOf<List<Tile>>()

        for (tile in board.activeTiles()) {
            if (tile.cell in visited) continue

            // BFS to find connected group of same value
            val group = mutableListOf<Tile>()
            val queue = ArrayDeque<HexCoord>()
            queue.add(tile.cell)
            visited.add(tile.cell)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val currentTile = board.tileAt(current) ?: continue

                if (currentTile.value == tile.value) {
                    group.add(currentTile)

                    // Visit six hex neighbors
                    for (neighbor in current.neighbors()) {
                        if (neighbor !in visited) {
                            val neighborTile = board.tileAt(neighbor)
                            if (neighborTile != null && neighborTile.value == tile.value) {
                                visited.add(neighbor)
                                queue.add(neighbor)
                            }
                        }
                    }
                }
            }

            if (group.size >= Constants.MIN_MERGE_COUNT) {
                groups.add(group)
            }
        }

        return groups
    }

    /**
     * Resolve a single merge group.
     * See Master Plan Section 11.
     *
     * - Pick merge destination (deterministic: prefer center-most cell)
     * - Remove source tiles
     * - Create upgraded tile at destination
     * - Calculate score
     *
     * @param board Current board state.
     * @param group The tiles to merge.
     * @param preferredDestination Optional preferred destination (e.g., newly placed tile).
     * @param chainIndex The chain step index for multiplier calculation.
     * @return Pair of the new board state and the merge event.
     */
    fun resolveGroup(
        board: BoardState,
        group: List<Tile>,
        preferredDestination: HexCoord? = null,
        chainIndex: Int = 0
    ): Pair<BoardState, GameEvent.MergeCompleted> {
        val mergedValue = group.first().value * 2
        val destination = selectDestination(group, preferredDestination)

        // Remove all source tiles
        var newBoard = board
        for (tile in group) {
            newBoard = newBoard.withoutTile(tile.cell)
        }

        // Create upgraded tile at destination
        val newTile = Tile(
            id = System.nanoTime(),
            value = mergedValue,
            cell = destination
        )
        newBoard = newBoard.withTile(newTile)

        // Calculate score
        val score = scoreEngine.calculateMergeScore(
            mergedValue = mergedValue,
            tileCount = group.size,
            chainIndex = chainIndex
        )

        val event = GameEvent.MergeCompleted(
            resultTile = newTile,
            mergedCount = group.size,
            scoreEarned = score
        )

        return newBoard to event
    }

    /**
     * Select the merge destination cell.
     * See Master Plan Section 11 — Merge destination.
     *
     * Rules (deterministic for replay consistency):
     * 1. If a preferred destination is in the group, use it.
     * 2. Otherwise, use the center-most cell (closest to origin).
     */
    private fun selectDestination(
        group: List<Tile>,
        preferred: HexCoord?
    ): HexCoord {
        // Prefer the specified destination if it's in the group
        if (preferred != null && group.any { it.cell == preferred }) {
            return preferred
        }

        // Otherwise pick center-most (closest to origin)
        return group.minByOrNull { it.cell.distanceTo(HexCoord.ORIGIN) }?.cell
            ?: group.first().cell
    }
}
