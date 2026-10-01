package com.mergeseven.game.economy

import com.mergeseven.game.game.modes.ModeIds

/** Free progression. Ads are bonuses, never an entry requirement. */
object RewardRules {
    const val COIN_AD_DAILY_LIMIT = 3
    const val HINT_AD_RUN_LIMIT = 3
    fun resultCoins(mode: String, won: Boolean, firstClear: Boolean, moves: Int, score: Long): Int =
        when {
            mode == ModeIds.CAMPAIGN && won -> if (firstClear) 100 else 30
            mode == ModeIds.CAMPAIGN && moves >= 10 -> 10
            mode in setOf(ModeIds.ENDLESS, ModeIds.TIME_ATTACK) && moves >= 10 ->
                (score / 100).coerceIn(0, 50).toInt()
            else -> 0
        }
}
