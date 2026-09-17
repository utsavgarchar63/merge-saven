package com.mergeseven.game.core.analytics

/**
 * Stable event names for AF0 scaffolding and AF6 Firebase wiring.
 * Do not rename — analytics dashboards will key off these strings.
 */
object AnalyticsEvents {
    const val APP_OPEN = "app_open"
    const val TUTORIAL_STARTED = "tutorial_started"
    const val TUTORIAL_COMPLETED = "tutorial_completed"
    const val GAME_START = "game_start"
    const val GAME_RESUMED = "game_resumed"
    const val PIECE_PLACED = "piece_placed"
    const val MERGE_COMPLETED = "merge_completed"
    const val CHAIN_COMPLETED = "chain_completed"
    const val LEVEL_STARTED = "level_started"
    const val LEVEL_COMPLETE = "level_complete"
    const val GAME_OVER = "game_over"
    const val BOOSTER_USED = "booster_used"
    const val UNDO_USED = "undo_used"
    const val HINT_USED = "hint_used"
    const val REWARD_AD_STARTED = "reward_ad_started"
    const val REWARD_AD_COMPLETED = "reward_ad_completed"
    const val CONTINUE_USED = "continue_used"
    const val DAILY_STARTED = "daily_started"
    const val DAILY_COMPLETED = "daily_completed"
    const val SHOP_OPENED = "shop_opened"
    const val PURCHASE_STARTED = "purchase_started"
    const val PURCHASE_COMPLETED = "purchase_completed"
    const val SETTINGS_CHANGED = "settings_changed"
    const val XP_GAINED = "xp_gained"
    const val ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
    const val SCORE_SUBMIT_ATTEMPTED = "score_submit_attempted"
    const val SCORE_SUBMIT_REJECTED = "score_submit_rejected"
    const val SCORE_SUBMIT_ACCEPTED = "score_submit_accepted"
    const val SHARE_RUN = "share_run"
    const val TOURNAMENT_JOINED = "tournament_joined"
    const val GHOST_REPLAY_STARTED = "ghost_replay_started"
}
