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

    @androidx.room.Transaction
    suspend fun seedBooster(id: String, starting: Int) {
        if (get(id) == null) upsert(UnlockEntity(id, UnlockEntity.CATEGORY_BOOSTER, starting, System.currentTimeMillis()))
    }

    @androidx.room.Transaction
    suspend fun addBooster(id: String, amount: Int) {
        if (amount <= 0) return
        val old = get(id)
        upsert(UnlockEntity(id, UnlockEntity.CATEGORY_BOOSTER, (old?.quantity ?: 0) + amount, System.currentTimeMillis()))
    }

    @androidx.room.Transaction
    suspend fun consumeBooster(id: String): Boolean {
        val old = get(id) ?: return false
        if (old.quantity <= 0) return false
        upsert(old.copy(quantity = old.quantity - 1))
        return true
    }

    @androidx.room.Transaction
    suspend fun grantOnce(claimId: String, boosterId: String, amount: Int): Boolean {
        if (get(claimId) != null) return false
        val old = get(boosterId)
        upsert(UnlockEntity(boosterId, UnlockEntity.CATEGORY_BOOSTER, (old?.quantity ?: 0) + amount, System.currentTimeMillis()))
        upsert(UnlockEntity(claimId, "reward_claim", 1, System.currentTimeMillis()))
        return true
    }

    @Query("DELETE FROM unlock WHERE unlockId = :unlockId")
    suspend fun delete(unlockId: String)

    @Query("DELETE FROM unlock")
    suspend fun clearAll()
}
