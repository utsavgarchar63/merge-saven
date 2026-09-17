package com.mergeseven.game.game.engine.traits

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TileTrait
import java.util.EnumMap

/**
 * Data-driven trait rules for AF1 (AF1-07). Engines call these instead of nesting trait `if`s.
 *
 * Tables below encode participation, bomb destruction, and thawability; connection / scoring
 * helpers compose those tables with group state.
 */
object TraitInteractions {

    private val canParticipate: Map<TileTrait, Boolean> = EnumMap<TileTrait, Boolean>(TileTrait::class.java).apply {
        put(TileTrait.NORMAL, true)
        put(TileTrait.BOMB, true)
        put(TileTrait.WILDCARD, true)
        put(TileTrait.FROZEN, false) // overridden when freezeStage == 0 (thawed → NORMAL)
        put(TileTrait.STONE, false)
        put(TileTrait.MULTIPLIER, true)
    }

    private val destroyedByBombClear: Map<TileTrait, Boolean> = EnumMap<TileTrait, Boolean>(TileTrait::class.java).apply {
        TileTrait.entries.forEach { put(it, true) }
    }

    private val thawable: Map<TileTrait, Boolean> = EnumMap<TileTrait, Boolean>(TileTrait::class.java).apply {
        put(TileTrait.NORMAL, false)
        put(TileTrait.BOMB, false)
        put(TileTrait.WILDCARD, false)
        put(TileTrait.FROZEN, true)
        put(TileTrait.STONE, false)
        put(TileTrait.MULTIPLIER, false)
    }

    private val removableByBooster: Map<TileTrait, Boolean> = EnumMap<TileTrait, Boolean>(TileTrait::class.java).apply {
        TileTrait.entries.forEach { put(it, true) }
    }

    fun canParticipateInMerge(tile: Tile): Boolean {
        if (tile.trait == TileTrait.FROZEN && tile.freezeStage > 0) return false
        if (tile.trait == TileTrait.FROZEN && tile.freezeStage <= 0) return true
        return canParticipate[tile.trait] == true
    }

    /**
     * Whether [neighbor] may join a growing group whose established non-wildcard value is
     * [groupValue] (null while the group is still all wildcards).
     */
    fun canConnect(neighbor: Tile, groupValue: Int?): Boolean {
        if (!canParticipateInMerge(neighbor)) return false
        if (neighbor.trait == TileTrait.WILDCARD) return true
        return groupValue == null || neighbor.value == groupValue
    }

    fun establishedValue(group: List<Tile>): Int {
        require(group.isNotEmpty()) { "empty merge group" }
        return group.firstOrNull { it.trait != TileTrait.WILDCARD }?.value
            ?: group.first().value
    }

    fun mergedResultValue(group: List<Tile>): Int = establishedValue(group) * 2

    /**
     * Product of MULTIPLIER factors in the group, times [Constants.BOMB_SCORE_FACTOR] if any bomb
     * was consumed.
     */
    fun scoreTraitMultiplier(group: List<Tile>): Float {
        var factor = 1f
        for (tile in group) {
            if (tile.trait == TileTrait.MULTIPLIER || tile.multiplierFactor > 1) {
                factor *= tile.multiplierFactor.coerceAtLeast(1).toFloat()
            }
        }
        if (group.any { it.trait == TileTrait.BOMB }) {
            factor *= Constants.BOMB_SCORE_FACTOR
        }
        return factor
    }

    fun applyThaw(tile: Tile): Tile {
        if (thawable[tile.trait] != true || tile.freezeStage <= 0) return tile
        val next = tile.freezeStage - 1
        return if (next <= 0) {
            tile.copy(trait = TileTrait.NORMAL, freezeStage = 0)
        } else {
            tile.copy(freezeStage = next)
        }
    }

    fun isDestroyedByBombClear(tile: Tile): Boolean =
        destroyedByBombClear[tile.trait] == true

    fun canRemoveWithBooster(tile: Tile): Boolean =
        removableByBooster[tile.trait] == true

    fun isWildcard(tile: Tile): Boolean = tile.trait == TileTrait.WILDCARD

    fun isBomb(tile: Tile): Boolean = tile.trait == TileTrait.BOMB

    fun isFrozen(tile: Tile): Boolean =
        tile.trait == TileTrait.FROZEN && tile.freezeStage > 0
}
