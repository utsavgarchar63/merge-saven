package com.mergeseven.game.game.solver

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prefers Remote Config A/B difficulty when AF6 is on (AF6-06); else local DataStore.
 */
@Singleton
class RemoteAwareDifficultyProfileProvider @Inject constructor(
    private val local: LocalDifficultyProfileProvider,
    private val liveConfig: LiveConfig,
    private val featureFlags: FeatureFlags,
    @PersistenceScope scope: CoroutineScope
) : DifficultyProfileProvider {

    override val profile: StateFlow<DifficultyProfile> =
        combine(local.profile, liveConfig.revision, featureFlags.observe(Feature.AF6)) { localProfile, _, af6 ->
            if (!af6) return@combine localProfile
            val remote = liveConfig.abDifficultyProfile() ?: return@combine localProfile
            DifficultyProfiles.of(remote)
        }.stateIn(scope, SharingStarted.Eagerly, DifficultyProfiles.STANDARD)

    override suspend fun setProfile(id: DifficultyId) {
        local.setProfile(id)
    }
}
