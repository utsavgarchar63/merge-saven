package com.mergeseven.game.ui.shop

import android.app.Activity
import androidx.lifecycle.*
import com.mergeseven.game.ads.*
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.economy.RewardRules
import com.mergeseven.game.game.model.BoosterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(private val userData: UserDataRepository,
    private val inventory: BoosterInventoryStore, val ads: AdService, private val date: DateProvider,
    private val liveConfig: LiveConfig) : ViewModel() {
    val profile = userData.userProfile
    val owned = inventory.owned
    val availability = ads.availability
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private fun prefix() = "${date.today()}:coin:"
    fun claimsLeft() = (RewardRules.COIN_AD_DAILY_LIMIT - userData.rewardCount(prefix())).coerceAtLeast(0)
    fun cost(type: BoosterType) = liveConfig.boosterCost(type)
    fun onOpened() {
        viewModelScope.launch { inventory.loadAndSeed() }
        ads.preload(AdPlacement.FUNDS_COINS)
    }
    fun watchEarnAd(activity: Activity) {
        if (_busy.value || claimsLeft() == 0) return
        if (!ads.isReady(AdPlacement.FUNDS_COINS)) {
            ads.preload(AdPlacement.FUNDS_COINS); _status.value = "No ad available right now. Play to earn coins."; return
        }
        _busy.value = true
        val prefix = prefix()
        val key = prefix + java.util.UUID.randomUUID()
        viewModelScope.launch {
            try {
                val result = ads.showRewarded(activity, AdPlacement.FUNDS_COINS) {
                    userData.claimReward(key, 100, prefix, RewardRules.COIN_AD_DAILY_LIMIT)
                }
                _status.value = if (result == AdResult.Rewarded) "+100 coins received" else "No reward claimed. You can keep playing."
            } finally { _busy.value = false }
        }
    }
    fun buyBooster(type: BoosterType) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val price = cost(type)
                if (userData.buyBooster(type, price)) {
                    inventory.loadAndSeed(); _status.value = "One ${type.name.lowercase().replace('_', ' ')} added"
                } else _status.value = "Earn more coins by playing, daily rewards, or an optional ad."
            } finally { _busy.value = false }
        }
    }
}
