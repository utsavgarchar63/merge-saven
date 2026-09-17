package com.mergeseven.game.core.liveops

import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.solver.DifficultyId
import kotlinx.coroutines.flow.StateFlow

/**
 * Typed live-ops config surface (AF6-04). Implementations never throw on bad JSON.
 */
interface LiveConfig {
    val revision: StateFlow<Long>

    fun spawnWeights(): Map<Int, Int>
    fun boosterCost(type: BoosterType): Int
    fun hintCost(): Int
    fun adFrequency(): Long
    fun featureFlagOverride(feature: Feature): Boolean?
    fun killSwitches(): KillSwitches
    fun abDifficultyProfile(): DifficultyId?
    fun abBoosterPriceMult(): Double
    fun levelPackManifestUrl(): String
    fun seasonalEvent(): SeasonalEventConfig?
    fun levelTargetOverride(level: Int): Int?
    /** AF9-09 remote offers; empty on corrupt JSON. */
    fun offersJson(): String
}

/**
 * Parses string maps (RC or in-memory) into typed values with safe fallbacks.
 */
class ParsedLiveConfig(
    private val strings: () -> Map<String, String>,
    override val revision: StateFlow<Long>
) : LiveConfig {

    override fun spawnWeights(): Map<Int, Int> {
        val raw = strings()[LiveConfigKeys.SPAWN_WEIGHTS_JSON].orEmpty()
        if (raw.isBlank()) return LiveConfigDefaults.spawnWeights()
        return runCatching {
            LiveConfigDefaults.json.decodeFromString<Map<String, Int>>(raw)
                .mapKeys { it.key.toInt() }
        }.getOrElse { LiveConfigDefaults.spawnWeights() }
    }

    override fun boosterCost(type: BoosterType): Int {
        val base = costMap()[type.name]
            ?: LiveConfigDefaults.boosterCosts()[type.name]
            ?: Constants.UNDO_COST
        return (base * abBoosterPriceMult()).toInt().coerceAtLeast(0)
    }

    override fun hintCost(): Int {
        val base = costMap()["HINT"] ?: Constants.HINT_COST
        return (base * abBoosterPriceMult()).toInt().coerceAtLeast(0)
    }

    override fun adFrequency(): Long =
        strings()[LiveConfigKeys.AD_FREQUENCY]?.toLongOrNull()
            ?: LiveConfigDefaults.adFrequency()

    override fun featureFlagOverride(feature: Feature): Boolean? {
        val raw = strings()[LiveConfigKeys.FEATURE_FLAGS_JSON].orEmpty()
        if (raw.isBlank()) return null
        val map = runCatching {
            LiveConfigDefaults.json.decodeFromString<Map<String, Boolean>>(raw)
        }.getOrNull() ?: return null
        return map[feature.name]
    }

    override fun killSwitches(): KillSwitches {
        val raw = strings()[LiveConfigKeys.KILL_SWITCHES_JSON].orEmpty()
        if (raw.isBlank()) return LiveConfigDefaults.killSwitches()
        return runCatching {
            LiveConfigDefaults.json.decodeFromString<KillSwitches>(raw)
        }.getOrElse { LiveConfigDefaults.killSwitches() }
    }

    override fun abDifficultyProfile(): DifficultyId? {
        val raw = strings()[LiveConfigKeys.AB_DIFFICULTY_PROFILE].orEmpty()
        if (raw.isBlank()) return null
        return runCatching { DifficultyId.valueOf(raw.uppercase()) }.getOrNull()
    }

    override fun abBoosterPriceMult(): Double =
        strings()[LiveConfigKeys.AB_BOOSTER_PRICE_MULT]?.toDoubleOrNull()
            ?.coerceIn(0.25, 4.0)
            ?: LiveConfigDefaults.abBoosterPriceMult()

    override fun levelPackManifestUrl(): String =
        strings()[LiveConfigKeys.LEVEL_PACK_MANIFEST_URL].orEmpty()

    override fun seasonalEvent(): SeasonalEventConfig? {
        val raw = strings()[LiveConfigKeys.SEASONAL_EVENT_JSON].orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            LiveConfigDefaults.json.decodeFromString<SeasonalEventConfig>(raw)
        }.getOrNull()?.takeIf { it.id.isNotBlank() }
    }

    override fun levelTargetOverride(level: Int): Int? {
        val raw = strings()[LiveConfigKeys.LEVEL_TARGETS_JSON].orEmpty()
        if (raw.isBlank()) return null
        val map = runCatching {
            LiveConfigDefaults.json.decodeFromString<Map<String, Int>>(raw)
        }.getOrNull() ?: return null
        return map[level.toString()]
    }

    override fun offersJson(): String =
        strings()[LiveConfigKeys.OFFERS_JSON].orEmpty().ifBlank {
            LiveConfigDefaults.offersJson()
        }

    private fun costMap(): Map<String, Int> {
        val raw = strings()[LiveConfigKeys.BOOSTER_COSTS_JSON].orEmpty()
        if (raw.isBlank()) return LiveConfigDefaults.boosterCosts()
        return runCatching {
            LiveConfigDefaults.json.decodeFromString<Map<String, Int>>(raw)
        }.getOrElse { LiveConfigDefaults.boosterCosts() }
    }
}

/** Deterministic in-memory config for unit tests. */
class InMemoryLiveConfig(
    initial: Map<String, String> = LiveConfigDefaults.defaultStringMap(),
    revisionFlow: StateFlow<Long>
) : LiveConfig by ParsedLiveConfig({ initial }, revisionFlow)
