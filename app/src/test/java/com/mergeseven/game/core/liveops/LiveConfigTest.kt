package com.mergeseven.game.core.liveops

import com.mergeseven.game.core.Constants
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.solver.DifficultyId
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveConfigTest {

    @Test
    fun defaultsMatchConstantsCostsAndWeights() {
        val config = ParsedLiveConfig(
            strings = { LiveConfigDefaults.defaultStringMap() },
            revision = MutableStateFlow(0L)
        )
        assertEquals(Constants.SPAWN_WEIGHTS, config.spawnWeights())
        assertEquals(Constants.UNDO_COST, config.boosterCost(BoosterType.UNDO))
        assertEquals(Constants.HINT_COST, config.hintCost())
        assertEquals(1.0, config.abBoosterPriceMult(), 0.0)
        assertNull(config.abDifficultyProfile())
        assertFalse(config.killSwitches().kill_ads)
    }

    @Test
    fun badJsonFallsBackToDefaults() {
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(LiveConfigKeys.SPAWN_WEIGHTS_JSON, "{not-json")
            put(LiveConfigKeys.BOOSTER_COSTS_JSON, "[]")
            put(LiveConfigKeys.KILL_SWITCHES_JSON, "nope")
            put(LiveConfigKeys.SEASONAL_EVENT_JSON, "{")
        }
        val config = ParsedLiveConfig({ strings }, MutableStateFlow(1L))
        assertEquals(Constants.SPAWN_WEIGHTS, config.spawnWeights())
        assertEquals(Constants.UNDO_COST, config.boosterCost(BoosterType.UNDO))
        assertFalse(config.killSwitches().kill_af4)
        assertNull(config.seasonalEvent())
    }

    @Test
    fun priceMultScalesBoosterCost() {
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(LiveConfigKeys.AB_BOOSTER_PRICE_MULT, "2.0")
        }
        val config = ParsedLiveConfig({ strings }, MutableStateFlow(0L))
        assertEquals(Constants.UNDO_COST * 2, config.boosterCost(BoosterType.UNDO))
    }

    @Test
    fun abDifficultyParses() {
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(LiveConfigKeys.AB_DIFFICULTY_PROFILE, "HARD")
        }
        val config = ParsedLiveConfig({ strings }, MutableStateFlow(0L))
        assertEquals(DifficultyId.HARD, config.abDifficultyProfile())
    }

    @Test
    fun killSwitchBlocksFeature() {
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(
                LiveConfigKeys.KILL_SWITCHES_JSON,
                """{"kill_af4":true,"kill_ads":true,"kill_iap":false}"""
            )
        }
        val config = ParsedLiveConfig({ strings }, MutableStateFlow(0L))
        val gates = object {
            fun allowed(f: Feature) = !config.killSwitches().blocks(f)
            fun ads() = !config.killSwitches().kill_ads
        }
        assertFalse(gates.allowed(Feature.AF4))
        assertTrue(gates.allowed(Feature.AF5))
        assertFalse(gates.ads())
    }

    @Test
    fun gatedFlagsHonourKillSwitchEvenWhenLocalOn() {
        val local = InMemoryFeatureFlags(
            isDebug = true,
            initial = mapOf(Feature.AF4 to true)
        )
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(LiveConfigKeys.KILL_SWITCHES_JSON, """{"kill_af4":true}""")
        }
        val revision = MutableStateFlow(0L)
        val liveConfig = ParsedLiveConfig({ strings }, revision)
        // Simulate GatedFeatureFlags kill path without Firebase.
        assertTrue(local.isEnabled(Feature.AF4))
        assertTrue(liveConfig.killSwitches().blocks(Feature.AF4))
        val effective = !liveConfig.killSwitches().blocks(Feature.AF4) && local.isEnabled(Feature.AF4)
        assertFalse(effective)
    }

    @Test
    fun seasonalEventParsesWhenPresent() {
        val json = """
            {"id":"summer","startIso":"2020-01-01T00:00:00Z","endIso":"2099-01-01T00:00:00Z",
             "boardThemeId":"wood","rewardTrack":[{"threshold":100,"coins":50}]}
        """.trimIndent()
        val strings = LiveConfigDefaults.defaultStringMap().toMutableMap().apply {
            put(LiveConfigKeys.SEASONAL_EVENT_JSON, json)
        }
        val event = ParsedLiveConfig({ strings }, MutableStateFlow(0L)).seasonalEvent()
        assertEquals("summer", event?.id)
        assertEquals(1, event?.rewardTrack?.size)
    }
}
