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
 * Prototype level pool.
 * See Master Plan Section 93.
 */
object LevelPool {
    val levels = listOf(
        LevelRule(level = 1, target = 16),
        LevelRule(level = 2, target = 32),
        LevelRule(level = 3, target = 64),
        LevelRule(level = 4, target = 128),
        LevelRule(level = 5, target = 256)
    )

    fun getLevel(level: Int): LevelRule {
        return levels.getOrElse(level - 1) {
            // For levels beyond the pool, scale the target
            LevelRule(
                level = level,
                target = (1L shl (level + 3)) // 2^(level+3)
            )
        }
    }
}
