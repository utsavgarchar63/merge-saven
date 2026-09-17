package com.mergeseven.game.core.flags

/**
 * Phase-level feature gates for advanced work (AF1–AF12).
 *
 * Every new AF feature entry point must check
 * `featureFlags.isEnabled(Feature.AFn)` before showing UI or running logic, so unfinished work
 * can ship dark. Defaults are always off; engineers flip a flag from the DEBUG menu.
 *
 * Finer-grained flags can be added later without changing [FeatureFlags].
 */
enum class Feature {
    /** Advanced gameplay: special tiles, board variety, objectives. */
    AF1,

    /** Game modes: Endless, Time Attack, Zen, Daily Puzzle, etc. */
    AF2,

    /** Booster economy and CONTINUE. */
    AF3,

    /** Hints, solver, adaptive difficulty. */
    AF4,

    /** Meta progression and collections. */
    AF5,

    /** Live-ops / Firebase remote config. */
    AF6,

    /** Accounts and cloud save. */
    AF7,

    /** Competitive and social. */
    AF8,

    /** Advanced monetization. */
    AF9,

    /** Presentation and game feel. */
    AF10,

    /** Accessibility and device reach. */
    AF11,

    /** Hardening: security, quality, delivery. */
    AF12
}
