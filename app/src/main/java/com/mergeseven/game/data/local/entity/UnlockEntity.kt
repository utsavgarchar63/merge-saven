package com.mergeseven.game.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Anything the player owns permanently: boosters, tile skins, board themes, achievements.
 *
 * Created now so AF3/AF5 can add unlock categories without a schema migration; [category] keeps
 * the namespaces separate.
 */
@Entity(tableName = "unlock")
data class UnlockEntity(
    @PrimaryKey
    val unlockId: String,
    val category: String,
    val quantity: Int,
    val unlockedAt: Long
) {
    companion object {
        const val CATEGORY_BOOSTER = "booster"
        const val CATEGORY_COSMETIC = "cosmetic"
        const val CATEGORY_ACHIEVEMENT = "achievement"
        const val CATEGORY_MILESTONE = "milestone"
        const val CATEGORY_MODE_RECORD = "mode_record"
        /** AF9: Remove Ads / Premium entitlements. */
        const val CATEGORY_ENTITLEMENT = "entitlement"
    }
}
