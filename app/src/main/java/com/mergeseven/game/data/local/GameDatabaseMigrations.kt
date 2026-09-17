package com.mergeseven.game.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object GameDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE level_progress ADD COLUMN failCount INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN playerLevel INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                "ALTER TABLE user_profile ADD COLUMN equippedTileThemeId TEXT NOT NULL DEFAULT 'classic'"
            )
            db.execSQL(
                "ALTER TABLE user_profile ADD COLUMN equippedBoardThemeId TEXT NOT NULL DEFAULT 'wood'"
            )
            db.execSQL("ALTER TABLE user_profile ADD COLUMN totalMerges INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN biggestTile INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN longestChain INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN playtimeMs INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
