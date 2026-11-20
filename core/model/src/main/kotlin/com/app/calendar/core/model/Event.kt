package com.app.calendar.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A user event. Times are wall-clock values in [zoneId]; all-day events ignore the time part and
 * span [start].toLocalDate() .. [end].toLocalDate() inclusive, independent of time zone.
 */
data class Event(
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val location: String = "",
    val isAllDay: Boolean,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zoneId: ZoneId,
    val color: EventColor = EventColor.LACQUER,
    val recurrence: Recurrence? = null,
    val reminders: List<Reminder> = emptyList(),
    /** Dates (occurrence start dates) removed from a recurring series. */
    val excludedDates: Set<LocalDate> = emptySet(),
) {
    val startDate: LocalDate get() = start.toLocalDate()
    val endDate: LocalDate get() = end.toLocalDate()
    val isRecurring: Boolean get() = recurrence != null
}

/** Remind [minutesBefore] minutes before the occurrence starts (all-day: before 00:00 of that day). */
@JvmInline
value class Reminder(val minutesBefore: Int) {
    companion object {
        val PRESETS = listOf(0, 5, 15, 30, 60, 24 * 60).map(::Reminder)
    }
}

/** Palette keys; the design system maps each to a light/dark colour and a Vietnamese label. */
enum class EventColor { LACQUER, RICE, APRICOT, RIVER, LOTUS, INK }

/** One concrete instance of an [Event] (a single date for non-recurring events). */
data class EventOccurrence(
    val event: Event,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val date: LocalDate get() = start.toLocalDate()

    /** Whether this occurrence touches [day] (multi-day events span several days). */
    fun covers(day: LocalDate): Boolean = !day.isBefore(start.toLocalDate()) && !day.isAfter(lastDay)

    val lastDay: LocalDate
        get() = if (event.isAllDay || end.toLocalTime() != java.time.LocalTime.MIDNIGHT || end.toLocalDate() == start.toLocalDate()) {
            end.toLocalDate()
        } else {
            end.toLocalDate().minusDays(1) // a timed event ending exactly at midnight does not occupy the next day
        }
}
