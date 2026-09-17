package com.mergeseven.game.core.liveops

import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.game.model.BoosterType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Built-in defaults that mirror [Constants] / local behaviour when RC is empty (AF6-04/05).
 */
object LiveConfigDefaults {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun spawnWeights(): Map<Int, Int> = Constants.SPAWN_WEIGHTS

    fun boosterCosts(): Map<String, Int> = mapOf(
        BoosterType.UNDO.name to Constants.UNDO_COST,
        BoosterType.SWAP.name to Constants.SWAP_COST,
        BoosterType.RANDOMIZE.name to Constants.RANDOMIZE_COST,
        BoosterType.REMOVE.name to Constants.REMOVE_COST,
        BoosterType.CONTINUE.name to Constants.CONTINUE_COST,
        BoosterType.HAMMER.name to Constants.HAMMER_COST,
        BoosterType.VALUE_UP.name to Constants.VALUE_UP_COST,
        BoosterType.MAGNET.name to Constants.MAGNET_COST,
        BoosterType.TIME_FREEZE.name to Constants.TIME_FREEZE_COST,
        "HINT" to Constants.HINT_COST
    )

    fun featureFlags(): Map<String, Boolean> =
        Feature.entries.associate { it.name to false }

    fun killSwitches(): KillSwitches = KillSwitches()

    fun seasonalEventJson(): String = ""

    fun levelPackManifestUrl(): String = ""

    fun abDifficultyProfile(): String = ""

    fun abBoosterPriceMult(): Double = 1.0

    fun adFrequency(): Long = 3L

    fun offersJson(): String = """
        [
          {"id":"starter","productId":"coins_500","title":"Starter Pack","subtitle":"500 coins to get going","kind":"starter","minDaysAway":0,"maxStreak":1},
          {"id":"comeback","productId":"coins_2000","title":"Welcome Back","subtitle":"2000 coins after time away","kind":"comeback","minDaysAway":3,"maxStreak":1},
          {"id":"streak_save","productId":"pack_undo","title":"Streak Save","subtitle":"Undo pack when streak is at risk","kind":"streak_save","minDaysAway":0,"maxStreak":1}
        ]
    """.trimIndent()

    fun defaultStringMap(): Map<String, String> = mapOf(
        LiveConfigKeys.SPAWN_WEIGHTS_JSON to json.encodeToString(
            spawnWeights().mapKeys { it.key.toString() }
        ),
        LiveConfigKeys.LEVEL_TARGETS_JSON to "{}",
        LiveConfigKeys.BOOSTER_COSTS_JSON to json.encodeToString(boosterCosts()),
        LiveConfigKeys.AD_FREQUENCY to adFrequency().toString(),
        LiveConfigKeys.FEATURE_FLAGS_JSON to json.encodeToString(featureFlags()),
        LiveConfigKeys.KILL_SWITCHES_JSON to json.encodeToString(killSwitches()),
        LiveConfigKeys.AB_DIFFICULTY_PROFILE to abDifficultyProfile(),
        LiveConfigKeys.AB_BOOSTER_PRICE_MULT to abBoosterPriceMult().toString(),
        LiveConfigKeys.LEVEL_PACK_MANIFEST_URL to levelPackManifestUrl(),
        LiveConfigKeys.SEASONAL_EVENT_JSON to seasonalEventJson(),
        LiveConfigKeys.OFFERS_JSON to offersJson()
    )
}

@Serializable
data class KillSwitches(
    val kill_ads: Boolean = false,
    val kill_iap: Boolean = false,
    val kill_af1: Boolean = false,
    val kill_af2: Boolean = false,
    val kill_af3: Boolean = false,
    val kill_af4: Boolean = false,
    val kill_af5: Boolean = false,
    val kill_af6: Boolean = false
) {
    fun blocks(feature: Feature): Boolean = when (feature) {
        Feature.AF1 -> kill_af1
        Feature.AF2 -> kill_af2
        Feature.AF3 -> kill_af3
        Feature.AF4 -> kill_af4
        Feature.AF5 -> kill_af5
        Feature.AF6 -> kill_af6
        else -> false
    }
}

@Serializable
data class SeasonalEventConfig(
    val id: String = "",
    val startIso: String = "",
    val endIso: String = "",
    val boardThemeId: String? = null,
    val tileThemeId: String? = null,
    val rewardTrack: List<SeasonalRewardStep> = emptyList()
)

@Serializable
data class SeasonalRewardStep(
    val threshold: Long,
    val coins: Int = 0,
    val cosmeticId: String? = null
)

@Serializable
data class LevelPackManifest(
    val levels: List<LevelPackEntry> = emptyList()
)

@Serializable
data class LevelPackEntry(
    val levelId: Int,
    val url: String,
    val sha256: String
)
