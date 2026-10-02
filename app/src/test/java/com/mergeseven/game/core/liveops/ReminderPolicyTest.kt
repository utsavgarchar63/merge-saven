package com.mergeseven.game.core.liveops

import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ReminderPolicyTest {
    @Test fun eveningAndTomorrowRespectLocalTime() {
        assertEquals(3_600_000L, ReminderPolicy.delayUntilEvening(ZonedDateTime.parse("2026-10-02T18:00:00+05:30")))
        assertEquals(86_400_000L, ReminderPolicy.delayUntilEvening(ZonedDateTime.parse("2026-10-02T19:00:00+05:30")))
    }
    @Test fun quietHoursAndActiveOrCompletedPlaySuppressReminder() {
        assertTrue(ReminderPolicy.shouldNotify(19, false, false, false))
        assertFalse(ReminderPolicy.shouldNotify(23, false, false, false))
        assertFalse(ReminderPolicy.shouldNotify(7, false, false, false))
        assertFalse(ReminderPolicy.shouldNotify(19, true, false, false))
        assertFalse(ReminderPolicy.shouldNotify(19, false, true, false))
        assertFalse(ReminderPolicy.shouldNotify(19, false, false, true))
    }
}
