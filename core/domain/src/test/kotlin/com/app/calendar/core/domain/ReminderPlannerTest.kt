package com.app.calendar.core.domain

import com.app.calendar.core.domain.recurrence.RecurrenceExpander
import com.app.calendar.core.domain.reminder.ReminderPlanner
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val planner = ReminderPlanner(RecurrenceExpander(LunarCalendar()))
    private fun at(local: String, zone: ZoneId = HCM): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `multiple reminders produce one trigger each`() {
        val event = timedEvent("2026-11-20T09:00", "2026-11-20T10:00", reminders = listOf(15, 60, 24 * 60))
        val triggers = planner.plan(listOf(event), at("2026-11-19T08:00"), Duration.ofDays(7), HCM)
        assertThat(triggers.map { it.triggerAt }).containsExactly(
            at("2026-11-19T09:00"), at("2026-11-20T08:00"), at("2026-11-20T08:45"),
        ).inOrder()
        assertThat(triggers.map { it.key }.toSet()).hasSize(3)
    }

    @Test
    fun `past triggers and triggers beyond the window are dropped`() {
        val event = timedEvent("2026-11-20T09:00", "2026-11-20T10:00", reminders = listOf(15, 60))
        val triggers = planner.plan(listOf(event), at("2026-11-20T08:30"), Duration.ofDays(1), HCM)
        assertThat(triggers.map { it.minutesBefore }).containsExactly(15)
    }

    @Test
    fun `recurring events are planned within the rolling window only`() {
        val event = timedEvent("2026-11-01T07:00", "2026-11-01T07:30", Recurrence(Frequency.DAILY), reminders = listOf(10))
        val triggers = planner.plan(listOf(event), at("2026-11-20T00:00"), Duration.ofDays(3), HCM)
        assertThat(triggers).hasSize(3)
    }

    @Test
    fun `timed events keep their own zone when the device zone changes`() {
        val event = timedEvent("2026-11-20T09:00", "2026-11-20T10:00", reminders = listOf(0))
        val tokyo = ZoneId.of("Asia/Tokyo")
        val trigger = planner.plan(listOf(event), at("2026-11-19T00:00"), Duration.ofDays(3), tokyo).single()
        assertThat(trigger.triggerAt).isEqualTo(at("2026-11-20T09:00", HCM))
    }

    @Test
    fun `all day reminders follow the device zone`() {
        val event = allDayEvent("2026-11-20", reminders = listOf(7 * 60))
        val tokyo = ZoneId.of("Asia/Tokyo")
        val trigger = planner.plan(listOf(event), at("2026-11-18T00:00"), Duration.ofDays(3), tokyo).single()
        assertThat(trigger.triggerAt).isEqualTo(at("2026-11-19T17:00", tokyo))
    }
}
