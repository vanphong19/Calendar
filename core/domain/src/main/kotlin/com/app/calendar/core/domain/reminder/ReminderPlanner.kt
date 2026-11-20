package com.app.calendar.core.domain.reminder

import com.app.calendar.core.domain.recurrence.RecurrenceExpander
import com.app.calendar.core.model.Event
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/** A single notification to fire for one reminder of one occurrence. */
data class ReminderTrigger(
    val eventId: Long,
    val eventTitle: String,
    val occurrenceStart: LocalDateTime,
    val isAllDay: Boolean,
    val minutesBefore: Int,
    val triggerAt: Instant,
) {
    /** Stable identity used to de-duplicate and cancel alarms. */
    val key: String get() = "$eventId|$occurrenceStart|$minutesBefore"
}

/** Computes which reminders fire within a rolling window. Pure and deterministic. */
class ReminderPlanner @Inject constructor(private val expander: RecurrenceExpander) {

    fun plan(events: List<Event>, now: Instant, horizon: Duration, deviceZone: ZoneId): List<ReminderTrigger> {
        val windowEnd = now.plus(horizon)
        return events.flatMap { event ->
            if (event.reminders.isEmpty()) return@flatMap emptyList()
            val maxLead = Duration.ofMinutes(event.reminders.maxOf { it.minutesBefore }.toLong().coerceAtLeast(0))
            val zone = if (event.isAllDay) deviceZone else event.zoneId
            val from = now.atZone(zone).toLocalDate().minusDays(1)
            val to = windowEnd.plus(maxLead).atZone(zone).toLocalDate().plusDays(1)
            expander.expand(event, from, to).flatMap { occurrence ->
                val startInstant = if (event.isAllDay) {
                    occurrence.date.atStartOfDay(deviceZone).toInstant()
                } else {
                    occurrence.start.atZone(event.zoneId).toInstant()
                }
                event.reminders.distinct().mapNotNull { reminder ->
                    val triggerAt = startInstant.minus(Duration.ofMinutes(reminder.minutesBefore.toLong()))
                    if (triggerAt.isAfter(now) && !triggerAt.isAfter(windowEnd)) {
                        ReminderTrigger(event.id, event.title, occurrence.start, event.isAllDay, reminder.minutesBefore, triggerAt)
                    } else {
                        null
                    }
                }
            }
        }.sortedBy { it.triggerAt }
    }
}
