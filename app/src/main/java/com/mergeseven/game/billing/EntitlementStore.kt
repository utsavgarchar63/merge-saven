package com.mergeseven.game.billing

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent Remove Ads / Premium entitlements (AF9-05/08/10).
 * Updates are immediate for UI (no restart).
 */
@Singleton
class EntitlementStore @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider
) {
    private val _removeAds = MutableStateFlow(false)
    private val _premium = MutableStateFlow(false)
    private val _removeAdsOrPremium = MutableStateFlow(false)

    val removeAds: StateFlow<Boolean> = _removeAds.asStateFlow()
    val premium: StateFlow<Boolean> = _premium.asStateFlow()
    val removeAdsOrPremium: StateFlow<Boolean> = _removeAdsOrPremium.asStateFlow()

    suspend fun load() = withContext(dispatchers.io) {
        val rows = unlockDao.getAll().filter { it.category == UnlockEntity.CATEGORY_ENTITLEMENT }
        val hasRemove = rows.any { it.unlockId == REMOVE_ADS_ID && it.quantity > 0 }
        val hasPremium = rows.any { it.unlockId == PREMIUM_ID && it.quantity > 0 }
        apply(hasRemove, hasPremium)
    }

    suspend fun grantRemoveAds() = withContext(dispatchers.io) {
        unlockDao.upsert(
            UnlockEntity(
                unlockId = REMOVE_ADS_ID,
                category = UnlockEntity.CATEGORY_ENTITLEMENT,
                quantity = 1,
                unlockedAt = System.currentTimeMillis()
            )
        )
        apply(removeAds = true, premium = _premium.value)
    }

    suspend fun grantPremium(active: Boolean) = withContext(dispatchers.io) {
        if (active) {
            unlockDao.upsert(
                UnlockEntity(
                    unlockId = PREMIUM_ID,
                    category = UnlockEntity.CATEGORY_ENTITLEMENT,
                    quantity = 1,
                    unlockedAt = System.currentTimeMillis()
                )
            )
        } else {
            unlockDao.delete(PREMIUM_ID)
        }
        apply(removeAds = _removeAds.value, premium = active)
    }

    private fun apply(removeAds: Boolean, premium: Boolean) {
        _removeAds.value = removeAds
        _premium.value = premium
        _removeAdsOrPremium.value = removeAds || premium
    }

    companion object {
        const val REMOVE_ADS_ID = "entitlement_remove_ads"
        const val PREMIUM_ID = "entitlement_premium"
    }
}

/** In-memory entitlements for tests. */
class FakeEntitlementStore {
    val removeAdsOrPremium = MutableStateFlow(false)
    val removeAds = MutableStateFlow(false)
    val premium = MutableStateFlow(false)

    fun grantRemoveAds() {
        removeAds.value = true
        removeAdsOrPremium.value = true
    }

    fun grantPremium(active: Boolean) {
        premium.value = active
        removeAdsOrPremium.value = removeAds.value || active
    }
}
