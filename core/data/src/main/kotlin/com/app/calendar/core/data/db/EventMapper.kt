package com.app.calendar.core.data.db

import com.app.calendar.core.model.Event
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import com.app.calendar.core.model.Reminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal fun EventWithDetails.toModel(): Event = with(event) {
    Event(
        id = id,
        title = title,
        note = note,
        location = location,
        isAllDay = isAllDay,
        start = dateTime(startEpochDay, startMinute),
        end = dateTime(endEpochDay, endMinute),
        zoneId = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()),
        color = runCatching { EventColor.valueOf(color) }.getOrDefault(EventColor.LACQUER),
        recurrence = recurrenceFrequency?.let { frequency ->
            Recurrence(
                frequency = Frequency.valueOf(frequency),
                interval = recurrenceInterval.coerceAtLeast(1),
                daysOfWeek = DayOfWeek.entries.filter { recurrenceDaysMask and (1 shl it.ordinal) != 0 }.toSet(),
                until = recurrenceUntilEpochDay?.let(LocalDate::ofEpochDay),
                count = recurrenceCount,
            )
        },
        reminders = reminders.map { Reminder(it.minutesBefore) }.sortedBy { it.minutesBefore },
        excludedDates = exclusions.map { LocalDate.ofEpochDay(it.epochDay) }.toSet(),
    )
}

internal fun Event.toEntity(now: Long): EventEntity = EventEntity(
    id = id,
    title = title,
    note = note,
    location = location,
    isAllDay = isAllDay,
    startEpochDay = start.toLocalDate().toEpochDay(),
    startMinute = if (isAllDay) 0 else start.toLocalTime().toSecondOfDay() / 60,
    endEpochDay = end.toLocalDate().toEpochDay(),
    endMinute = if (isAllDay) 0 else end.toLocalTime().toSecondOfDay() / 60,
    zoneId = zoneId.id,
    color = color.name,
    recurrenceFrequency = recurrence?.frequency?.name,
    recurrenceInterval = recurrence?.interval ?: 1,
    recurrenceDaysMask = recurrence?.daysOfWeek?.fold(0) { mask, day -> mask or (1 shl day.ordinal) } ?: 0,
    recurrenceUntilEpochDay = recurrence?.until?.toEpochDay(),
    recurrenceCount = recurrence?.count,
    updatedAt = now,
)

private fun dateTime(epochDay: Long, minute: Int): LocalDateTime =
    LocalDate.ofEpochDay(epochDay).atTime(LocalTime.ofSecondOfDay(minute * 60L))
