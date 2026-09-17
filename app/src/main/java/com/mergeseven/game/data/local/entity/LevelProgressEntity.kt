package com.mergeseven.game.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-level progression. A row exists only for levels the player has completed at least once
 * or failed enough times to track (AF4-07 failCount).
 */
@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey
    val levelNumber: Int,
    val stars: Int,
    val bestScore: Long,
    val isCompleted: Boolean,
    val updatedAt: Long,
    /** Campaign lose count for adaptive spawn (AF4-07). */
    val failCount: Int = 0
)
