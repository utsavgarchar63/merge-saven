package com.mergeseven.game.ui.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.ads.AdPlacement
import com.mergeseven.game.ads.AdPreloader
import com.mergeseven.game.ads.AdResult
import com.mergeseven.game.ads.AdService
import com.mergeseven.game.billing.BillingRepository
import com.mergeseven.game.billing.EntitlementStore
import com.mergeseven.game.billing.OfferEngine
import com.mergeseven.game.billing.ProductCatalog
import com.mergeseven.game.billing.PurchaseResult
import com.mergeseven.game.billing.RemoteOffer
import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.core.liveops.LiveOpsGates
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.model.BoosterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopOffer(
    val id: String,
    val title: String,
    val subtitle: String,
    val priceLabel: String = "",
    val comingSoon: Boolean = true
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val boosterInventory: BoosterInventoryStore,
    private val analyticsTracker: AnalyticsTracker,
    private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val billingRepository: BillingRepository,
    private val adService: AdService,
    private val adPreloader: AdPreloader,
    private val entitlementStore: EntitlementStore,
    private val offerEngine: OfferEngine
) : ViewModel() {

    val coins: StateFlow<Int> = userDataRepository.userProfile
        .map { it.coins }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val af3Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF3, viewModelScope)
    val af5Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF5, viewModelScope)
    val af9Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF9, viewModelScope)
    val removeAdsOwned: StateFlow<Boolean> = entitlementStore.removeAdsOrPremium

    private val _offers = MutableStateFlow(defaultOffers(comingSoon = true))
    val offers: StateFlow<List<ShopOffer>> = _offers.asStateFlow()

    private val _remoteOffer = MutableStateFlow<RemoteOffer?>(null)
    val remoteOffer: StateFlow<RemoteOffer?> = _remoteOffer.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun onOpened() {
        analyticsTracker.logEvent(AnalyticsEvents.SHOP_OPENED)
        adPreloader.warmShop()
        viewModelScope.launch {
            if (featureFlags.isEnabled(Feature.AF9) && liveOpsGates.iapAllowed()) {
                billingRepository.start()
                val products = billingRepository.queryProducts()
                _offers.value = ProductCatalog.all.map { catalog ->
                    val price = products.firstOrNull { it.productId == catalog.productId }?.priceLabel.orEmpty()
                    ShopOffer(
                        id = catalog.productId,
                        title = catalog.title,
                        subtitle = catalog.subtitle,
                        priceLabel = price.ifBlank { "Buy" },
                        comingSoon = false
                    )
                }
                _remoteOffer.value = offerEngine.currentOffer()
            } else {
                _offers.value = defaultOffers(comingSoon = true)
            }
        }
    }

    fun buy(activity: Activity, productId: String) {
        if (!featureFlags.isEnabled(Feature.AF9) || !liveOpsGates.iapAllowed()) {
            _status.value = "Purchases unavailable"
            return
        }
        viewModelScope.launch {
            when (val result = billingRepository.purchase(activity, productId)) {
                is PurchaseResult.Success -> _status.value = "Purchased ${result.productId}"
                PurchaseResult.Cancelled -> _status.value = "Purchase cancelled"
                PurchaseResult.Pending -> _status.value = "Purchase pending"
                PurchaseResult.Unavailable -> _status.value = "Billing unavailable"
                is PurchaseResult.Failed -> _status.value = result.message
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            val ids = billingRepository.restorePurchases()
            _status.value = if (ids.isEmpty()) "No purchases to restore" else "Restored ${ids.size} item(s)"
        }
    }

    fun watchEarnAd(activity: Activity) {
        if (!featureFlags.isEnabled(Feature.AF9)) {
            grantStubReward()
            return
        }
        viewModelScope.launch {
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_STARTED, mapOf("source" to "shop"))
            when (adService.showRewarded(activity, AdPlacement.FUNDS_COINS)) {
                AdResult.Rewarded -> {
                    analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_COMPLETED, mapOf("source" to "shop"))
                    userDataRepository.addCoins(Constants.REWARDED_COIN_GRANT)
                    boosterInventory.grant(BoosterType.UNDO, 1)
                    _status.value = "+${Constants.REWARDED_COIN_GRANT} coins"
                }
                else -> _status.value = "No ad available"
            }
        }
    }

    /** Stub earn path when AF9 is off. */
    fun grantStubReward() {
        userDataRepository.addCoins(Constants.REWARDED_COIN_GRANT)
        viewModelScope.launch { boosterInventory.grant(BoosterType.UNDO, 1) }
    }

    private fun defaultOffers(comingSoon: Boolean) = ProductCatalog.all.map {
        ShopOffer(it.productId, it.title, it.subtitle, comingSoon = comingSoon)
    }
}
