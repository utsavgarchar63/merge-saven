package com.mergeseven.game.game.engine

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.traits.TraitInteractions
import com.mergeseven.game.game.model.*

/**
 * Outcome of resolving one merge group, including side-effect inputs for the chain layer.
 */
data class MergeResolution(
    val board: BoardState,
    val event: GameEvent.MergeCompleted,
    val bombSourceCells: List<HexCoord>,
    val mergeCells: Set<HexCoord>,
    val destination: HexCoord
)

/**
 * Detects and resolves tile merges on the board.
 * See Master Plan Sections 10-11 (Merge Rule, Merge Resolution), Phase 8 / AF1.
 *
 * Uses BFS to find connected mergeable groups (wildcard-aware via [TraitInteractions]).
 * Groups of MIN_MERGE_COUNT or more are merged into the next value.
 */
class MergeEngine(
    private val scoreEngine: ScoreEngine
) {

    /**
     * Find all mergeable groups on the board.
     * A group is a set of connected merge-participating tiles that share one established value
     * (wildcards match any) and has at least MIN_MERGE_COUNT members.
     */
    fun findMergeableGroups(board: BoardState): List<List<Tile>> {
        val visited = mutableSetOf<HexCoord>()
        val groups = mutableListOf<List<Tile>>()

        val ordered = board.activeTiles().sortedWith(compareBy({ it.cell.q }, { it.cell.r }, { it.id }))

        // Non-wildcard seeds first so group value is stable and wildcards are claimed deterministically.
        for (start in ordered) {
            if (start.cell in visited) continue
            if (!TraitInteractions.canParticipateInMerge(start)) continue
            if (TraitInteractions.isWildcard(start)) continue

            val group = growGroup(board, start, start.value, visited)
            if (group.size >= Constants.MIN_MERGE_COUNT) {
                groups.add(group)
            }
        }

        // Remaining all-wildcard clusters.
        for (start in ordered) {
            if (start.cell in visited) continue
            if (!TraitInteractions.canParticipateInMerge(start)) continue
            if (!TraitInteractions.isWildcard(start)) continue

            val group = growGroup(board, start, groupValue = null, visited)
            if (group.size >= Constants.MIN_MERGE_COUNT) {
                groups.add(group)
            }
        }

        return groups
    }

    private fun growGroup(
        board: BoardState,
        start: Tile,
        groupValue: Int?,
        visited: MutableSet<HexCoord>
    ): List<Tile> {
        val group = mutableListOf<Tile>()
        val queue = ArrayDeque<Tile>()
        var value = groupValue

        queue.add(start)
        visited.add(start.cell)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            group.add(current)

            if (value == null && !TraitInteractions.isWildcard(current)) {
                value = current.value
            }

            for (neighborCoord in current.cell.neighbors()) {
                if (neighborCoord in visited) continue
                val neighbor = board.tileAt(neighborCoord) ?: continue
                if (!TraitInteractions.canConnect(neighbor, value)) continue

                visited.add(neighborCoord)
                queue.add(neighbor)
                if (value == null && !TraitInteractions.isWildcard(neighbor)) {
                    value = neighbor.value
                }
            }
        }

        return group
    }

    /**
     * Resolve a single merge group.
     * Result tile is always [TileTrait.NORMAL]. Bomb clears and thaw are applied by
     * [ChainReactionEngine] using [MergeResolution] metadata.
     */
    fun resolveGroup(
        board: BoardState,
        group: List<Tile>,
        random: GameRandom,
        preferredDestination: HexCoord? = null,
        chainIndex: Int = 0
    ): MergeResolution {
        val mergedValue = TraitInteractions.mergedResultValue(group)
        val destination = selectDestination(group, preferredDestination)
        val mergeCells = group.map { it.cell }.toSet()
        val bombSources = group.filter { TraitInteractions.isBomb(it) }.map { it.cell }

        var newBoard = board
        for (tile in group) {
            newBoard = newBoard.withoutTile(tile.cell)
        }

        val newTile = Tile(
            id = random.nextId(),
            value = mergedValue,
            cell = destination,
            trait = TileTrait.NORMAL
        )
        newBoard = newBoard.withTile(newTile)

        val traitMultiplier = TraitInteractions.scoreTraitMultiplier(group)
        val padBonus = board.modifierAt(destination)
            ?.takeIf { it.type == com.mergeseven.game.game.model.CellModifierType.SCORE_PAD }
            ?.scoreBonus
            ?: 1f
        val score = scoreEngine.calculateMergeScore(
            mergedValue = mergedValue,
            tileCount = group.size,
            chainIndex = chainIndex,
            traitMultiplier = traitMultiplier * padBonus
        )

        val event = GameEvent.MergeCompleted(
            resultTile = newTile,
            mergedCount = group.size,
            scoreEarned = score
        )

        return MergeResolution(
            board = newBoard,
            event = event,
            bombSourceCells = bombSources,
            mergeCells = mergeCells,
            destination = destination
        )
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
        if (preferred != null && group.any { it.cell == preferred }) {
            return preferred
        }

        return group.minByOrNull { it.cell.distanceTo(HexCoord.ORIGIN) }?.cell
            ?: group.first().cell
    }
}
