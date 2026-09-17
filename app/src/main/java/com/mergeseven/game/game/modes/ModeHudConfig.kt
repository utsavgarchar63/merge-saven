package com.mergeseven.game.game.modes

/**
 * HUD / session policy flags for a [GameMode] (AF2-01).
 */
data class ModeHudConfig(
    val showObjectives: Boolean = false,
    val showTimer: Boolean = false,
    val showTarget: Boolean = false,
    val allowGameOver: Boolean = true,
    val unlimitedUndo: Boolean = false,
    val suppressAds: Boolean = false,
    val paletteKey: String = "default",
    val musicKey: String = "default"
)
