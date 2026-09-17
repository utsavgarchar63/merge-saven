package com.mergeseven.game.core.debug

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DebugCommand {
    data object ForceGameOver : DebugCommand
}

/**
 * Cross-screen debug bus. The Settings debug menu posts commands; [GameViewModel] (when alive)
 * consumes them. [forceGameOver] returns false when no game session is registered so the UI can
 * show "open a game first".
 */
@Singleton
class DebugCommands @Inject constructor() {

    private val activeGameSessions = AtomicInteger(0)

    private val _commands = MutableSharedFlow<DebugCommand>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val commands: SharedFlow<DebugCommand> = _commands.asSharedFlow()

    fun registerGameSession() {
        activeGameSessions.incrementAndGet()
    }

    fun unregisterGameSession() {
        activeGameSessions.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun hasActiveGameSession(): Boolean = activeGameSessions.get() > 0

    /** @return true if a game session should receive the command. */
    fun forceGameOver(): Boolean {
        if (!hasActiveGameSession()) return false
        return _commands.tryEmit(DebugCommand.ForceGameOver)
    }
}
