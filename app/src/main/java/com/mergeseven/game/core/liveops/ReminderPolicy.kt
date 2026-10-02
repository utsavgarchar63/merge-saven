package com.mergeseven.game.core.liveops

import java.time.Duration
import java.time.ZonedDateTime

object ReminderPolicy {
    fun delayUntilEvening(now: ZonedDateTime): Long {
        var next = now.withHour(19).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
    fun shouldNotify(hour: Int, foreground: Boolean, alreadySent: Boolean, puzzleCompleted: Boolean) =
        hour in 8..20 && !foreground && !alreadySent && !puzzleCompleted
}
