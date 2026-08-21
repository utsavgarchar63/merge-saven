package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.*

/**
 * Handles chain reactions — repeated merges until the board is stable.
 * See Master Plan Section 12.
 *
 * After a placement, merges may create new groups that also qualify for merging.
 * This engine loops until no more merges are possible.
 *
 * Guard: Each merge strictly reduces tile count, preventing infinite loops.
 */
class ChainReactionEngine(
    private val mergeEngine: MergeEngine
) {

    /**
     * Result of chain reaction resolution.
     */
    data class ChainResult(
        val finalBoard: BoardState,
        val events: List<GameEvent>,
        val chainLength: Int,
        val totalScore: Long
    )

    /**
     * Resolve all chain reactions on the board.
     * See Master Plan Section 12 pseudo-code.
     *
     * @param board The current board state.
     * @param recentlyPlaced Tiles that were just placed (used to prefer merge destination).
     * @return ChainResult with the final board and all events.
     */
    fun resolveChains(
        board: BoardState,
        recentlyPlaced: List<Tile> = emptyList()
    ): ChainResult {
        var currentBoard = board
        val allEvents = mutableListOf<GameEvent>()
        var chainIndex = 0
        var totalScore = 0L
        val maxIterations = 100 // Safety guard against infinite loops

        while (chainIndex < maxIterations) {
            val groups = mergeEngine.findMergeableGroups(currentBoard)

            if (groups.isEmpty()) break

            for (group in groups) {
                // For the first merge, prefer the recently placed tile as destination
                val preferred = if (chainIndex == 0) {
                    recentlyPlaced.firstOrNull { placed ->
                        group.any { it.cell == placed.cell }
                    }?.cell
                } else null

                val (newBoard, mergeEvent) = mergeEngine.resolveGroup(
                    board = currentBoard,
                    group = group,
                    preferredDestination = preferred,
                    chainIndex = chainIndex
                )
                currentBoard = newBoard
                allEvents.add(
                    GameEvent.MergeStarted(
                        sourceTiles = group,
                        destinationCoord = mergeEvent.resultTile.cell,
                        mergedValue = mergeEvent.resultTile.value
                    )
                )
                allEvents.add(mergeEvent)
                totalScore += mergeEvent.scoreEarned
            }

            chainIndex++
        }

        // Emit chain completed event if there was more than one step
        if (chainIndex > 1) {
            allEvents.add(
                GameEvent.ChainCompleted(
                    chainLength = chainIndex,
                    totalScoreEarned = totalScore
                )
            )
        }

        return ChainResult(
            finalBoard = currentBoard,
            events = allEvents,
            chainLength = chainIndex,
            totalScore = totalScore
        )
    }
}
