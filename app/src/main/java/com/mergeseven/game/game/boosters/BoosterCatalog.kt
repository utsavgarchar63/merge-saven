package com.mergeseven.game.game.boosters

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.model.GameState

/**
 * Per-booster economy + validation config (AF3-01).
 */
data class BoosterSpec(
    val type: BoosterType,
    val coinCost: Int,
    val cooldownMs: Long = 0L,
    val requiresTargetCell: Boolean = false,
    /** null = all modes; otherwise only listed mode ids. */
    val modesAllowed: Set<String>? = null,
    val startingOwned: Int = 0
)

data class BoosterUseContext(
    val owned: Int,
    val coins: Int,
    val nowMs: Long,
    val lastUsedMs: Long = 0L,
    val unlimitedUndo: Boolean = false,
    val undosUsedThisRun: Int = 0,
    val undoBlockedUntilMove: Boolean = false,
    val continuesUsed: Int = 0,
    val rewardedContinuesUsed: Int = 0
)

enum class BoosterDenyReason {
    OK,
    WRONG_MODE,
    COOLDOWN,
    NO_TARGET_NEEDED_BUT_BUSY,
    GAME_OVER,
    UNDO_EMPTY,
    UNDO_BLOCKED,
    UNDO_CAP,
    CONTINUE_CAP,
    INSUFFICIENT_FUNDS
}

object BoosterCatalog {

    val specs: Map<BoosterType, BoosterSpec> = listOf(
        BoosterSpec(BoosterType.UNDO, Constants.UNDO_COST, startingOwned = 3),
        BoosterSpec(BoosterType.SWAP, Constants.SWAP_COST, startingOwned = 2),
        BoosterSpec(BoosterType.RANDOMIZE, Constants.RANDOMIZE_COST, startingOwned = 2),
        BoosterSpec(BoosterType.REMOVE, Constants.REMOVE_COST, startingOwned = 1),
        BoosterSpec(BoosterType.CONTINUE, Constants.CONTINUE_COST, startingOwned = 0),
        BoosterSpec(
            BoosterType.HAMMER,
            Constants.HAMMER_COST,
            requiresTargetCell = true,
            startingOwned = 1
        ),
        BoosterSpec(
            BoosterType.VALUE_UP,
            Constants.VALUE_UP_COST,
            requiresTargetCell = true,
            startingOwned = 1
        ),
        BoosterSpec(
            BoosterType.MAGNET,
            Constants.MAGNET_COST,
            requiresTargetCell = true,
            startingOwned = 0
        ),
        BoosterSpec(
            BoosterType.TIME_FREEZE,
            Constants.TIME_FREEZE_COST,
            modesAllowed = setOf(ModeIds.TIME_ATTACK),
            startingOwned = 1
        )
    ).associateBy { it.type }

    fun spec(type: BoosterType): BoosterSpec = specs.getValue(type)

    fun continueCoinCost(continuesUsed: Int): Int {
        var cost = Constants.CONTINUE_COST
        repeat(continuesUsed.coerceAtLeast(0)) {
            cost = (cost * 2).coerceAtMost(Constants.CONTINUE_COST_CAP)
        }
        return cost
    }

    fun canUse(
        type: BoosterType,
        state: GameState,
        ctx: BoosterUseContext,
        costOverride: Int? = null
    ): BoosterDenyReason {
        val spec = spec(type)
        if (spec.modesAllowed != null && state.modeId !in spec.modesAllowed) {
            return BoosterDenyReason.WRONG_MODE
        }
        if (spec.cooldownMs > 0 && ctx.nowMs - ctx.lastUsedMs < spec.cooldownMs) {
            return BoosterDenyReason.COOLDOWN
        }

        return when (type) {
            BoosterType.UNDO -> when {
                ctx.unlimitedUndo -> if (state.previousState == null) {
                    BoosterDenyReason.UNDO_EMPTY
                } else {
                    BoosterDenyReason.OK
                }
                ctx.undoBlockedUntilMove -> BoosterDenyReason.UNDO_BLOCKED
                ctx.undosUsedThisRun >= Constants.MAX_UNDOS_PER_RUN -> BoosterDenyReason.UNDO_CAP
                state.previousState == null -> BoosterDenyReason.UNDO_EMPTY
                else -> affordability(type, ctx, costOverride)
            }
            BoosterType.CONTINUE -> when {
                !state.isGameOver -> BoosterDenyReason.GAME_OVER
                ctx.continuesUsed >= Constants.MAX_COIN_CONTINUES_PER_RUN &&
                    ctx.rewardedContinuesUsed >= Constants.REWARDED_CONTINUE_LIMIT ->
                    BoosterDenyReason.CONTINUE_CAP
                else -> {
                    val cost = costOverride ?: continueCoinCost(ctx.continuesUsed)
                    if (ctx.owned > 0 || ctx.coins >= cost) BoosterDenyReason.OK
                    else BoosterDenyReason.INSUFFICIENT_FUNDS
                }
            }
            else -> {
                if (state.isGameOver && type != BoosterType.CONTINUE) {
                    return BoosterDenyReason.GAME_OVER
                }
                affordability(type, ctx, costOverride)
            }
        }
    }

    private fun affordability(
        type: BoosterType,
        ctx: BoosterUseContext,
        costOverride: Int? = null
    ): BoosterDenyReason {
        if (ctx.owned > 0) return BoosterDenyReason.OK
        val cost = costOverride ?: spec(type).coinCost
        return if (ctx.coins >= cost) BoosterDenyReason.OK else BoosterDenyReason.INSUFFICIENT_FUNDS
    }

    fun unlockId(type: BoosterType): String = "booster_${type.name.lowercase()}"
}
