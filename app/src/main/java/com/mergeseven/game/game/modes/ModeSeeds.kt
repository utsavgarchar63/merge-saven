package com.mergeseven.game.game.modes

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/** Deterministic seeds for Daily / Weekly (AF2-06/07). */
object ModeSeeds {

    fun dailySeed(dateIso: String): Long =
        dateIso.hashCode().toLong() xor 0xD41A1L

    /**
     * AF4-06 helper — prefers a salt-adjusted seed when [resolver] is provided.
     */
    fun dailySeedResolved(
        dateIso: String,
        targetScore: Int,
        resolver: ((String, Int) -> Long)?
    ): Long = resolver?.invoke(dateIso, targetScore) ?: dailySeed(dateIso)

    fun weekKey(dateIso: String): String {
        val date = runCatching { LocalDate.parse(dateIso) }.getOrElse { LocalDate.now() }
        val week = date.get(WeekFields.of(Locale.ROOT).weekOfWeekBasedYear())
        val year = date.get(WeekFields.of(Locale.ROOT).weekBasedYear())
        return "%04d-W%02d".format(year, week)
    }

    fun weeklySeed(weekKey: String): Long =
        weekKey.hashCode().toLong() xor 0x57EE501L

    fun formatToday(date: LocalDate = LocalDate.now()): String =
        date.format(DateTimeFormatter.ISO_DATE)
}
