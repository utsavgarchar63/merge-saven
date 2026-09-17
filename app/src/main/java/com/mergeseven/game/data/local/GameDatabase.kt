package com.mergeseven.game.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mergeseven.game.data.local.dao.ActiveGameDao
import com.mergeseven.game.data.local.dao.LevelProgressDao
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.dao.UserProfileDao
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.data.local.entity.DailyQuestEntity
import com.mergeseven.game.data.local.entity.LevelProgressEntity
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.data.local.entity.UserProfileEntity

/**
 * Local persistence for everything the player would be upset to lose: wallet, progression,
 * unlocks, and the in-progress game.
 *
 * Schemas are exported to `app/schemas` and committed. Never bump [VERSION] without adding a
 * `Migration` and a matching case in `GameDatabaseMigrationTest` — destructive fallback is
 * deliberately not enabled, because it would silently wipe purchased content.
 */
@Database(
    entities = [
        ActiveGameEntity::class,
        LevelProgressEntity::class,
        UserProfileEntity::class,
        DailyQuestEntity::class,
        UnlockEntity::class
    ],
    version = GameDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {

    abstract fun activeGameDao(): ActiveGameDao

    abstract fun levelProgressDao(): LevelProgressDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun unlockDao(): UnlockDao

    companion object {
        const val VERSION = 3
        const val NAME = "merge_seven.db"
    }
}
