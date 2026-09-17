package com.mergeseven.game.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AF10-09: Macrobenchmark smoke for cold start + idle home frame timing.
 * Run on a device/emulator: `./gradlew :benchmark:connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.DEFAULT
    ) {
        startActivityAndWait()
    }

    @Test
    fun homeIdleFrames() = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.DEFAULT
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5_000)
        // Hold on Home so frame timing covers ambient UI / AF10 shimmer when enabled.
        device.waitForIdle(2_000)
    }

    companion object {
        private const val PACKAGE = "com.mergeseven.game"
    }
}
