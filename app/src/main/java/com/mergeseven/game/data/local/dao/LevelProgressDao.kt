package com.mergeseven.game.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mergeseven.game.data.local.entity.LevelProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAll(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    suspend fun getAll(): List<LevelProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LevelProgressEntity)

    @Query("SELECT COALESCE(SUM(stars), 0) FROM level_progress")
    suspend fun totalStars(): Int

    @Query("SELECT COALESCE(MAX(levelNumber), 0) FROM level_progress WHERE isCompleted = 1")
    suspend fun highestCompletedLevel(): Int

    @Query("DELETE FROM level_progress")
    suspend fun clear()
}
