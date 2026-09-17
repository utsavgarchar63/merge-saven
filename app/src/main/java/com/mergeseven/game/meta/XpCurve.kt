package com.mergeseven.game.meta

/**
 * XP curve: xp required to advance from [level] to level+1 is `100 * level` (AF5-01).
 */
object XpCurve {
    fun xpToNext(level: Int): Int = 100 * level.coerceAtLeast(1)

    /**
     * Applies [amount] XP and returns the updated (xp, playerLevel, levelsGained).
     */
    fun apply(currentXp: Int, currentLevel: Int, amount: Int): Triple<Int, Int, Int> {
        if (amount <= 0) return Triple(currentXp, currentLevel, 0)
        var xp = currentXp + amount
        var level = currentLevel.coerceAtLeast(1)
        var gained = 0
        while (xp >= xpToNext(level)) {
            xp -= xpToNext(level)
            level++
            gained++
        }
        return Triple(xp, level, gained)
    }

    fun progressFraction(xp: Int, level: Int): Float {
        val need = xpToNext(level)
        if (need <= 0) return 1f
        return (xp.toFloat() / need).coerceIn(0f, 1f)
    }
}
