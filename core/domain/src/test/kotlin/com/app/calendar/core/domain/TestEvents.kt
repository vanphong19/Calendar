package com.app.calendar.core.domain

import com.app.calendar.core.model.Event
import com.app.calendar.core.model.Recurrence
import com.app.calendar.core.model.Reminder
import java.time.LocalDateTime
import java.time.ZoneId

val HCM: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

fun timedEvent(
    start: String,
    end: String,
    recurrence: Recurrence? = null,
    reminders: List<Int> = emptyList(),
    id: Long = 1,
) = Event(
    id = id,
    title = "Sự kiện $id",
    isAllDay = false,
    start = LocalDateTime.parse(start),
    end = LocalDateTime.parse(end),
    zoneId = HCM,
    recurrence = recurrence,
    reminders = reminders.map(::Reminder),
)

fun allDayEvent(start: String, end: String = start, recurrence: Recurrence? = null, reminders: List<Int> = emptyList()) = Event(
    id = 2,
    title = "Cả ngày",
    isAllDay = true,
    start = LocalDateTime.parse("${start}T00:00"),
    end = LocalDateTime.parse("${end}T00:00"),
    zoneId = HCM,
    recurrence = recurrence,
    reminders = reminders.map(::Reminder),
)
