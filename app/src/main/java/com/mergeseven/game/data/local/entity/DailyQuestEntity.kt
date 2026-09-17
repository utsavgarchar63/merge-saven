package com.mergeseven.game.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A quest offered on a given day. Rows are replaced wholesale when the day rolls over.
 *
 * @param orderIndex preserves the display order the quests were generated in.
 */
@Entity(tableName = "daily_quest")
data class DailyQuestEntity(
    @PrimaryKey
    val questId: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val coinsReward: Int,
    val starsReward: Int,
    val isClaimed: Boolean,
    val orderIndex: Int
)
