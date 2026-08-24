package com.mergeseven.game.game.rules

import kotlinx.serialization.Serializable

/**
 * Level configuration.
 * See Master Plan Section 24, Section 93.
 *
 * Defines the goal and constraints for each level.
 */
@Serializable
data class LevelRule(
    /** Level number. */
    val level: Int,

    /** Target value or score to complete the level. */
    val target: Long,

    /** Type of goal for this level. */
    val goalType: LevelGoalType = LevelGoalType.REACH_TILE_VALUE,

    /** Optional starting board preset identifier. */
    val startingBoardPreset: String = "default",

    /** Spawn profile identifier for this level. */
    val spawnProfile: String = "default",

    /** Board profile (radius, blocked cells, etc.). */
    val boardProfile: String = "default"
)

/**
 * Types of level goals.
 * See Master Plan Section 24.
 */
@Serializable
enum class LevelGoalType {
    /** Reach a specific tile value. */
    REACH_TILE_VALUE,

    /** Reach a target score. */
    REACH_SCORE,

    /** Survive for N moves. */
    SURVIVE_MOVES,

    /** Create N merges. */
    CREATE_MERGES,

    /** Complete a special board configuration. */
    SPECIAL_BOARD
}

/**
 * Level pool with balanced target tile goals per level.
 */
object LevelPool {
    private val targetSequence = listOf(
        16L, 32L, 64L, 128L, 256L,
        512L, 1024L, 2048L, 4096L, 8192L
    )

    fun getLevel(level: Int): LevelRule {
        val target = if (level in 1..targetSequence.size) {
            targetSequence[level - 1]
        } else {
            // For levels 11+, target doubles up to max 65536
            val exp = (level + 3).coerceAtMost(16)
            1L shl exp
        }

        return LevelRule(
            level = level,
            target = target
        )
    }
}
