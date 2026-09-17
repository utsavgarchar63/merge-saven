package com.mergeseven.game.billing

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.liveops.LiveConfigDefaults
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.data.repository.UserDataRepository
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RemoteOffer(
    val id: String,
    val productId: String,
    val title: String,
    val subtitle: String = "",
    /** starter | comeback | streak_save */
    val kind: String,
    val minDaysAway: Int = 3,
    val maxStreak: Int = 1
)

/**
 * Surfaces at most one RC-driven offer (AF9-09).
 */
@Singleton
class OfferEngine @Inject constructor(
    private val liveConfig: LiveConfig,
    private val featureFlags: FeatureFlags,
    private val userDataRepository: UserDataRepository
) {
    fun currentOffer(profile: UserProfile = userDataRepository.userProfile.value): RemoteOffer? {
        if (!featureFlags.isEnabled(Feature.AF9)) return null
        val offers = parseOffers()
        if (offers.isEmpty()) return null
        val starter = offers.firstOrNull { it.kind == "starter" }
        if (starter != null && profile.totalMerges == 0 && profile.totalStars == 0) {
            return starter
        }
        val daysAway = daysSinceLastLogin(profile.lastLoginDate)
        val comeback = offers.firstOrNull { it.kind == "comeback" }
        if (comeback != null && daysAway >= comeback.minDaysAway) {
            return comeback
        }
        val streak = offers.firstOrNull { it.kind == "streak_save" }
        if (streak != null && profile.currentStreak <= streak.maxStreak && profile.currentStreak > 0) {
            return streak
        }
        return null
    }

    private fun parseOffers(): List<RemoteOffer> {
        val raw = liveConfig.offersJson()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            LiveConfigDefaults.json.decodeFromString<List<RemoteOffer>>(raw)
        }.getOrElse { emptyList() }
    }

    private fun daysSinceLastLogin(lastLogin: String): Long {
        if (lastLogin.isBlank()) return Long.MAX_VALUE
        val last = runCatching { LocalDate.parse(lastLogin) }.getOrNull() ?: return Long.MAX_VALUE
        return ChronoUnit.DAYS.between(last, LocalDate.now()).coerceAtLeast(0)
    }
}
