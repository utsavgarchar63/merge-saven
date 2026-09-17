package com.mergeseven.game.core.flags

import com.mergeseven.game.core.liveops.LiveOpsGates
import com.mergeseven.game.core.liveops.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeatureFlags that apply RC kill switches and optional RC force-on/off (AF6-11 / AF6-04).
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
        if (override != null && feature == Feature.AF6) {
            // AF6 itself can be forced; other features still need local DEBUG enable unless RC forces.
        }
        if (override == true) return true
        if (override == false) return false
        return local.isEnabled(feature)
    }

    override fun observe(feature: Feature): Flow<Boolean> =
        combine(
            local.observe(feature),
            remoteConfigRepository.revision
        ) { localOn, _ ->
            if (!liveOpsGates.isFeatureAllowed(feature)) return@combine false
            val override = remoteConfigRepository.liveConfig.featureFlagOverride(feature)
            when (override) {
                true -> true
                false -> false
                null -> localOn
            }
        }

    override suspend fun setEnabled(feature: Feature, enabled: Boolean) {
        local.setEnabled(feature, enabled)
    }

    override fun snapshot(): Map<Feature, Boolean> =
        Feature.entries.associateWith { isEnabled(it) }
}
