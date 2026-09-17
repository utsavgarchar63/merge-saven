package com.mergeseven.game.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.data.local.entity.UserProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Migration harness for [GameDatabase] (AF0-03).
 *
 * It exists before the first migration on purpose: the cost of adding a migration is what makes
 * people skip it, so the scaffolding is in place and the schema is exported from version 1 onward.
 *
 * When you add version N+1:
 * 1. Add the `Migration` object to [GameDatabase].
 * 2. Add a test that opens N, writes a row, runs the migration, and asserts the row survived.
 * 3. Keep [migrateAll] passing — it walks every migration in sequence.
 */
@RunWith(AndroidJUnit4::class)
class GameDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GameDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Walks the whole migration chain from the oldest schema to the current one. With a single
     * version this only proves the exported schema matches the code, which is itself the check that
     * catches an entity changed without a version bump.
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            GameDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build()

        database.openHelper.writableDatabase.close()
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2AddsFailCount() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO level_progress (levelNumber, stars, bestScore, isCompleted, updatedAt)
                VALUES (3, 2, 500, 1, 1000)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT failCount FROM level_progress WHERE levelNumber = 3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3AddsAf5ProfileFields() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO user_profile (
                    id, coins, totalStars, currentStreak, claimedDays, lastLoginDate,
                    challenge_dateSeed, challenge_title, challenge_targetScore,
                    challenge_isCompleted, challenge_bestScore, challenge_coinsReward,
                    challenge_starsReward, challenge_attempts
                ) VALUES (
                    1, 250, 0, 1, '[]', '',
                    '', 'DAILY CHALLENGE', 3000,
                    0, 0, 500,
                    5, 0
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, *ALL_MIGRATIONS).use { db ->
            db.query(
                """
                SELECT xp, playerLevel, equippedTileThemeId, equippedBoardThemeId,
                       totalMerges, biggestTile, longestChain, playtimeMs
                FROM user_profile WHERE id = 1
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals("classic", cursor.getString(2))
                assertEquals("wood", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
                assertEquals(0, cursor.getInt(5))
                assertEquals(0, cursor.getInt(6))
                assertEquals(0L, cursor.getLong(7))
            }
        }
    }

    @Test
    fun writesAndReadsBackOnTheCurrentSchema() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            GameDatabase::class.java
        ).build()

        val profile = UserProfileEntity(
            coins = 1_000,
            totalStars = 12,
            currentStreak = 3,
            claimedDays = setOf(1, 2, 3),
            lastLoginDate = "2026-01-01",
            dailyChallenge = com.mergeseven.game.data.local.entity.DailyChallengeEmbedded(
                dateSeed = "2026-01-01",
                title = "DAILY CHALLENGE",
                targetScore = 3_000,
                isCompleted = false,
                bestScore = 900,
                coinsReward = 500,
                starsReward = 5,
                attempts = 2
            )
        )

        database.userProfileDao().save(profile, emptyList())
        val loaded = database.userProfileDao().getProfile()

        assertEquals(profile, loaded)
        // The Set<Int> converter is the only non-trivial mapping in the schema.
        assertEquals(setOf(1, 2, 3), loaded?.claimedDays)

        database.close()
    }

    @Test
    fun unlockRowsRoundTrip() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            GameDatabase::class.java
        ).build()

        val unlock = UnlockEntity(
            unlockId = "booster_undo",
            category = UnlockEntity.CATEGORY_BOOSTER,
            quantity = 3,
            unlockedAt = 1_700_000_000_000L
        )
        database.unlockDao().upsert(unlock)

        assertEquals(unlock, database.unlockDao().get("booster_undo"))

        database.unlockDao().delete("booster_undo")
        assertNull(database.unlockDao().get("booster_undo"))

        database.close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"

        val ALL_MIGRATIONS = com.mergeseven.game.data.local.GameDatabaseMigrations.ALL
    }
}
