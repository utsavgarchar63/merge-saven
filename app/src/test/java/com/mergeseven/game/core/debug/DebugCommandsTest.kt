package com.mergeseven.game.core.debug

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugCommandsTest {

    @Test
    fun `force game over is rejected when no session is active`() {
        val commands = DebugCommands()

        assertFalse(commands.forceGameOver())
        assertFalse(commands.hasActiveGameSession())
    }

    @Test
    fun `force game over is accepted when a session is registered`() = runTest {
        val commands = DebugCommands()
        var received: DebugCommand? = null

        commands.registerGameSession()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            commands.commands.collect { received = it }
        }

        assertTrue(commands.forceGameOver())
        assertEquals(DebugCommand.ForceGameOver, received)
    }

    @Test
    fun `unregister clears the session so later force fails`() {
        val commands = DebugCommands()
        commands.registerGameSession()
        commands.unregisterGameSession()

        assertFalse(commands.forceGameOver())
    }
}
