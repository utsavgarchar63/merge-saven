package com.mergeseven.game.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mergeseven.game.data.local.entity.UnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockDao {

    @Query("SELECT * FROM unlock WHERE category = :category")
    fun observeCategory(category: String): Flow<List<UnlockEntity>>

    @Query("SELECT * FROM unlock")
    suspend fun getAll(): List<UnlockEntity>

    @Query("SELECT * FROM unlock WHERE unlockId = :unlockId")
    suspend fun get(unlockId: String): UnlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UnlockEntity)

    @Query("DELETE FROM unlock WHERE unlockId = :unlockId")
    suspend fun delete(unlockId: String)

    @Query("DELETE FROM unlock")
    suspend fun clearAll()
}
