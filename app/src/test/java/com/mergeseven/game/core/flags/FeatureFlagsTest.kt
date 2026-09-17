package com.mergeseven.game.core.flags

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {

    @Test
    fun `AF1 is off by default so unfinished work ships dark`() {
        val flags = InMemoryFeatureFlags(isDebug = true)

        assertFalse(flags.isEnabled(Feature.AF1))
        assertFalse(flags.requireEnabled(Feature.AF1))
        assertTrue(flags.snapshot().values.none { it })
    }

    @Test
    fun `every phase flag defaults to off`() {
        val flags = InMemoryFeatureFlags(isDebug = true)

        Feature.entries.forEach { feature ->
            assertFalse("$feature should start off", flags.isEnabled(feature))
        }
    }

    @Test
    fun `debug builds can toggle a flag on and off`() = runTest {
        val flags = InMemoryFeatureFlags(isDebug = true)

        flags.setEnabled(Feature.AF2, true)
        assertTrue(flags.isEnabled(Feature.AF2))
        assertTrue(flags.observe(Feature.AF2).first())

        flags.setEnabled(Feature.AF2, false)
        assertFalse(flags.isEnabled(Feature.AF2))
    }

    @Test
    fun `release builds hard-return false even after a write`() = runTest {
        val flags = InMemoryFeatureFlags(isDebug = false)

        flags.setEnabled(Feature.AF1, true)

        assertFalse(flags.isEnabled(Feature.AF1))
        assertFalse(flags.observe(Feature.AF1).first())
        assertEquals(false, flags.snapshot()[Feature.AF1])
    }

    @Test
    fun `release snapshot is all false regardless of initial map`() {
        val flags = InMemoryFeatureFlags(
            isDebug = false,
            initial = mapOf(Feature.AF6 to true)
        )

        assertTrue(flags.snapshot().values.none { it })
    }
}
