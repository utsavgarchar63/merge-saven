package com.mergeseven.game.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Supplies today's date as an ISO string (`yyyy-MM-dd`).
 *
 * Daily streaks, quests, and challenge seeds all key off this. Injecting it keeps that logic
 * testable without waiting for midnight, and gives AF12-05 a single place to move date resolution
 * from the device clock to a trusted source.
 */
fun interface DateProvider {
    fun today(): String
}

class SystemDateProvider : DateProvider {
    override fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
}
