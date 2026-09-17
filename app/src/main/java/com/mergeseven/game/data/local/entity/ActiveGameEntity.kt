package com.mergeseven.game.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persisted in-progress game.
 *
 * The board itself is stored as a serialized [com.mergeseven.game.data.local.snapshot.GameStateSnapshot]
 * rather than as normalized tile rows: the snapshot is only ever read and written whole, and keeping it
 * opaque means adding a gameplay field does not require a database migration.
 *
 * [slotId] allows one saved game per mode (AF2). Campaign uses [CAMPAIGN_SLOT].
 *
 * @param schemaVersion snapshot format version, checked on load so an incompatible save is discarded
 *   instead of crashing the app.
 */
@Entity(tableName = "active_game")
data class ActiveGameEntity(
    @PrimaryKey
    val slotId: String,
    val levelId: Int,
    val schemaVersion: Int,
    val snapshotJson: String,
    val updatedAt: Long
) {
    companion object {
        const val CAMPAIGN_SLOT = "campaign"
        const val ENDLESS_SLOT = "endless"
        const val TIME_ATTACK_SLOT = "time_attack"
        const val ZEN_SLOT = "zen"
        const val DAILY_SLOT = "daily"
        const val WEEKLY_SLOT = "weekly"
    }
}
