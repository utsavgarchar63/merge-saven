package com.mergeseven.game.game.engine

import com.mergeseven.game.game.engine.traits.TraitInteractions
import com.mergeseven.game.game.model.*

/**
 * Handles chain reactions — repeated merges until the board is stable.
 * See Master Plan Section 12 / AF1 (thaw + bomb AOE after each merge).
 *
 * After a placement, merges may create new groups that also qualify for merging.
 * This engine loops until no more merges are possible.
 *
 * Guard: Each merge strictly reduces tile count; thaw/bomb clear do not increment chainIndex alone.
 */
class ChainReactionEngine(
    private val mergeEngine: MergeEngine
) {

    data class ChainResult(
        val finalBoard: BoardState,
        val events: List<GameEvent>,
        val chainLength: Int,
        val totalScore: Long
    )

    fun resolveChains(
        board: BoardState,
        random: GameRandom,
        recentlyPlaced: List<Tile> = emptyList()
    ): ChainResult {
        var currentBoard = board
        val allEvents = mutableListOf<GameEvent>()
        var chainIndex = 0
        var totalScore = 0L
        val maxIterations = 100

        while (chainIndex < maxIterations) {
            val groups = mergeEngine.findMergeableGroups(currentBoard)

            if (groups.isEmpty()) break

            for (group in groups) {
                val preferred = if (chainIndex == 0) {
                    recentlyPlaced.firstOrNull { placed ->
                        group.any { it.cell == placed.cell }
                    }?.cell
                } else {
                    null
                }

                val resolution = mergeEngine.resolveGroup(
                    board = currentBoard,
                    group = group,
                    random = random,
                    preferredDestination = preferred,
                    chainIndex = chainIndex
                )
                currentBoard = resolution.board

                allEvents.add(
                    GameEvent.MergeStarted(
                        sourceTiles = group,
                        destinationCoord = resolution.destination,
                        mergedValue = resolution.event.resultTile.value
                    )
                )
                allEvents.add(resolution.event)
                totalScore += resolution.event.scoreEarned

                currentBoard = applyAdjacentThaws(currentBoard, resolution.mergeCells, allEvents)
                currentBoard = unlockAdjacentLocks(currentBoard, resolution.mergeCells)
                currentBoard = applyBombClears(currentBoard, resolution.bombSourceCells, allEvents)
            }

            chainIndex++
        }

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

    private fun applyAdjacentThaws(
        board: BoardState,
        mergeCells: Set<HexCoord>,
        events: MutableList<GameEvent>
    ): BoardState {
        var result = board
        val thawed = mutableListOf<Tile>()

        for (tile in board.activeTiles()) {
            if (!TraitInteractions.isFrozen(tile)) continue
            val adjacentToMerge = tile.cell.neighbors().any { it in mergeCells }
            if (!adjacentToMerge) continue

            val next = TraitInteractions.applyThaw(tile)
            if (next != tile) {
                result = result.withoutTile(tile.cell).withTile(next)
                thawed += next
            }
        }

        if (thawed.isNotEmpty()) {
            events.add(GameEvent.TilesThawed(thawed))
        }
        return result
    }

    /** Clears LOCKED modifiers on cells adjacent to the merged group (AF1-12). */
    private fun unlockAdjacentLocks(
        board: BoardState,
        mergeCells: Set<HexCoord>
    ): BoardState {
        var result = board
        for ((coord, modifier) in board.cellModifiers) {
            if (modifier.type != CellModifierType.LOCKED) continue
            val adjacentToMerge = coord.neighbors().any { it in mergeCells }
            if (adjacentToMerge) {
                result = result.withModifierCleared(coord)
            }
        }
        return result
    }

    private fun applyBombClears(
        board: BoardState,
        bombSourceCells: List<HexCoord>,
        events: MutableList<GameEvent>
    ): BoardState {
        if (bombSourceCells.isEmpty()) return board

        var result = board
        val cleared = mutableListOf<Tile>()
        val clearedCells = mutableSetOf<HexCoord>()

        for (bombCell in bombSourceCells) {
            for (neighbor in bombCell.neighbors()) {
                if (neighbor in clearedCells) continue
                if (!result.isPlayable(neighbor)) continue
                val tile = result.tileAt(neighbor) ?: continue
                if (!TraitInteractions.isDestroyedByBombClear(tile)) continue

                cleared += tile
                clearedCells += neighbor
                result = result.withoutTile(neighbor)
            }
        }

        if (cleared.isNotEmpty()) {
            events.add(GameEvent.TilesCleared(cleared, reason = "bomb"))
        }
        return result
    }
}
