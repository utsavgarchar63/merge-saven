package com.mergeseven.game.game.modes

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameModeRegistry @Inject constructor(
    campaignMode: CampaignMode,
    endlessMode: EndlessMode,
    timeAttackMode: TimeAttackMode,
    zenMode: ZenMode,
    dailyMode: DailyMode,
    weeklyMode: WeeklyMode
) {
    private val modes: Map<String, GameMode> = listOf(
        campaignMode,
        endlessMode,
        timeAttackMode,
        zenMode,
        dailyMode,
        weeklyMode
    ).associateBy { it.id }

    fun get(id: String): GameMode =
        modes[id] ?: modes.getValue(ModeIds.CAMPAIGN)

    fun all(): Collection<GameMode> = modes.values
}
