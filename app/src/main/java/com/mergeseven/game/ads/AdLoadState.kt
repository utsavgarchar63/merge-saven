package com.mergeseven.game.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AdLoadState {
    CHECKING_PRIVACY, PRIVACY_REQUIRED, LOADING, READY, UNAVAILABLE, DISABLED;

    val canRequest: Boolean get() = this in setOf(READY, UNAVAILABLE, PRIVACY_REQUIRED)
    fun actionLabel(ready: Boolean, offer: String): String = if (ready) offer else when (this) {
        CHECKING_PRIVACY -> "Checking ad availability…"
        LOADING -> "Loading ad…"
        PRIVACY_REQUIRED -> "Check ad availability"
        DISABLED -> "Ads unavailable"
        else -> "Try loading ad"
    }
}

/** One rewarded slot has one observable state for every reward placement. */
internal class AdLoadStateStore {
    private val states = MutableStateFlow(AdPlacement.entries.associateWith { AdLoadState.CHECKING_PRIVACY })
    val flow = states.asStateFlow()
    fun set(placement: AdPlacement, state: AdLoadState) {
        val placements = if (inventoryKey(placement) == AdPlacement.FUNDS_COINS) rewardedPlacements else setOf(placement)
        states.update { current -> current + placements.associateWith { state } }
    }
    fun reset(state: AdLoadState) { states.value = AdPlacement.entries.associateWith { state } }
}
