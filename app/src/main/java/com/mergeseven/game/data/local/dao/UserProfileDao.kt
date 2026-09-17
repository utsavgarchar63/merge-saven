package com.mergeseven.game.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mergeseven.game.data.local.entity.DailyQuestEntity
import com.mergeseven.game.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getProfile(id: Int = UserProfileEntity.SINGLETON_ID): UserProfileEntity?

    @Query("SELECT * FROM daily_quest ORDER BY orderIndex ASC")
    suspend fun getQuests(): List<DailyQuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuests(entities: List<DailyQuestEntity>)

    @Query("DELETE FROM daily_quest")
    suspend fun clearQuests()

    /**
     * Profile and quests always move together (a day rollover rewrites both), so they are written
     * in one transaction to avoid a half-applied daily reset if the process dies mid-write.
     */
    @Transaction
    suspend fun save(profile: UserProfileEntity, quests: List<DailyQuestEntity>) {
        upsertProfile(profile)
        clearQuests()
        upsertQuests(quests)
    }
}
