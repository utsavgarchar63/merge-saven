package com.mergeseven.game.core

/**
 * Common Kotlin extension functions used across the project.
 */

/**
 * Returns the value if it satisfies the predicate, or null otherwise.
 * Useful for inline conditional chains.
 */
inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    return if (predicate(this)) this else null
}

/**
 * Formats a large number with K/M suffixes for compact display.
 * e.g., 1500 -> "1.5K", 1000000 -> "1M"
 */
fun Long.formatCompact(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}

/**
 * Clamps an integer value between min and max inclusive.
 */
fun Int.clamp(min: Int, max: Int): Int {
    return coerceIn(min, max)
}
