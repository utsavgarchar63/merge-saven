package com.mergeseven.game.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveGameDao {

    @Query("SELECT * FROM active_game WHERE slotId = :slotId")
    fun observe(slotId: String): Flow<ActiveGameEntity?>

    @Query("SELECT * FROM active_game WHERE slotId = :slotId")
    suspend fun get(slotId: String): ActiveGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveGameEntity)

    @Query("DELETE FROM active_game WHERE slotId = :slotId")
    suspend fun delete(slotId: String)
}
