package com.mergeseven.game.core.liveops

import com.mergeseven.game.core.flags.Feature
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production kill switches for ads / IAP / AF features (AF6-11).
 */
@Singleton
class LiveOpsGates @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository
) {
    private val liveConfig: LiveConfig get() = remoteConfigRepository.liveConfig

    fun adsAllowed(): Boolean = !liveConfig.killSwitches().kill_ads

    fun iapAllowed(): Boolean = !liveConfig.killSwitches().kill_iap

    fun isFeatureAllowed(feature: Feature): Boolean =
        !liveConfig.killSwitches().blocks(feature)
}
