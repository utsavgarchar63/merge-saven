package com.mergeseven.game.billing

import com.mergeseven.game.cloud.CloudEconomyNotifier
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.meta.CosmeticCatalog
import com.mergeseven.game.meta.UnlockService
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.premiumStipendStore by preferencesDataStore("premium_stipend")

/**
 * Idempotent entitlement / consumable grants after validated purchases (AF9-05/07/08).
 */
@Singleton
class PurchaseGranter @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val boosterInventory: BoosterInventoryStore,
    private val entitlementStore: EntitlementStore,
    private val unlockService: UnlockService,
    private val cloudEconomyNotifier: CloudEconomyNotifier,
    private val featureFlags: FeatureFlags,
    private val dateProvider: DateProvider,
    @ApplicationContext private val context: Context
) {
    private val seenTokens = mutableSetOf<String>()
    private val stipendDayKey = stringPreferencesKey("last_stipend_day")

    suspend fun grant(productId: String, purchaseToken: String): Boolean {
        if (!seenTokens.add(purchaseToken)) return false
        val product = ProductCatalog.byId(productId) ?: return false
        when (product.kind) {
            ProductKind.CONSUMABLE_COINS -> userDataRepository.addCoins(product.coins)
            ProductKind.CONSUMABLE_BOOSTERS -> {
                product.boosters.forEach { (type, qty) ->
                    boosterInventory.grant(type, qty)
                }
            }
            ProductKind.NON_CONSUMABLE_REMOVE_ADS -> entitlementStore.grantRemoveAds()
            ProductKind.SUBSCRIPTION_PREMIUM -> {
                entitlementStore.grantPremium(active = true)
                unlockService.grantCosmetic(CosmeticCatalog.TILE_PREMIUM)
            }
        }
        notifyPurchase()
        return true
    }

    suspend fun claimPremiumDailyStipend(): Boolean {
        if (!entitlementStore.premium.value) return false
        val today = dateProvider.today()
        val prefs = context.premiumStipendStore.data.first()
        if (prefs[stipendDayKey] == today) return false
        context.premiumStipendStore.edit { it[stipendDayKey] = today }
        userDataRepository.addCoins(Constants.PREMIUM_DAILY_STIPEND_COINS)
        notifyPurchase()
        return true
    }

    private fun notifyPurchase() {
        if (featureFlags.isEnabled(Feature.AF7) || featureFlags.isEnabled(Feature.AF9)) {
            cloudEconomyNotifier.notifyChanged()
        }
    }
}
