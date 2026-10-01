package com.mergeseven.game.core.flags

import com.mergeseven.game.core.liveops.LiveOpsGates
import com.mergeseven.game.core.liveops.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shipping defaults with Remote Config restrictions. Debug local overrides remain useful.
 *
 * DEBUG local toggles still drive the base value; kill switches always win.
 */
@Singleton
class GatedFeatureFlags @Inject constructor(
    private val local: LocalFeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val remoteConfigRepository: RemoteConfigRepository
) : FeatureFlags {

    override fun isEnabled(feature: Feature): Boolean {
        if (!liveOpsGates.isFeatureAllowed(feature)) return false
        val override = remoteConfigRepository.liveConfig.featureFlagOverride(feature)
        return override != false && local.isEnabled(feature)
    }

    override fun observe(feature: Feature): Flow<Boolean> =
        combine(
            local.observe(feature),
            remoteConfigRepository.revision
        ) { localOn, _ ->
            if (!liveOpsGates.isFeatureAllowed(feature)) return@combine false
            val override = remoteConfigRepository.liveConfig.featureFlagOverride(feature)
            override != false && localOn
        }

    override suspend fun setEnabled(feature: Feature, enabled: Boolean) {
        local.setEnabled(feature, enabled)
    }

    override fun snapshot(): Map<Feature, Boolean> =
        Feature.entries.associateWith { isEnabled(it) }
}
