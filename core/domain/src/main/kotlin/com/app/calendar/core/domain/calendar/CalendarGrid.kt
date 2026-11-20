package com.app.calendar.core.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

object CalendarGrid {
    /** Dates shown in a month grid: whole weeks covering [month], always 6 rows for stable layout. */
    fun monthDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return List(42) { first.plusDays(it.toLong()) }
    }

    fun weekDates(anyDay: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val first = anyDay.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return List(7) { first.plusDays(it.toLong()) }
    }

    fun orderedWeekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = List(7) { firstDayOfWeek.plus(it.toLong()) }
}
