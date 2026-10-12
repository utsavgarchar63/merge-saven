package com.mergeseven.game.ui.feel

/** Presentation timings only; engine resolution, timers and persistence never depend on these. */
object GameMotion {
    const val ROTATE_MS = 160
    const val MERGE_MS = 260
    const val SELECT_MS = 140
    const val LANDING_DAMPING = 0.78f
    const val LANDING_STIFFNESS = 650f
}
