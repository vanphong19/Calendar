package com.app.calendar.core.domain.recurrence

import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.Event
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Expands events into concrete occurrences for a date range. Occurrences are computed on the fly
 * and never stored, so editing a series is a single row update.
 */
class RecurrenceExpander @Inject constructor(private val lunarCalendar: LunarCalendar) {

    /** Occurrences of [event] that touch [from]..[to] (inclusive), sorted by start. */
    fun expand(event: Event, from: LocalDate, to: LocalDate): List<EventOccurrence> {
        val duration = Duration.between(event.start, event.end).coerceAtLeast(Duration.ZERO)
        val spanDays = ChronoUnit.DAYS.between(event.startDate, event.endDate)
        // An occurrence starting up to spanDays before [from] can still touch the range.
        val earliestStart = from.minusDays(spanDays)

        return startDates(event)
            .takeWhile { !it.isAfter(to) }
            .filter { !it.isBefore(earliestStart) && it !in event.excludedDates }
            .map { date ->
                val start = date.atTime(event.start.toLocalTime())
                EventOccurrence(event, start, start.plus(duration))
            }
            .filter { occurrence -> !occurrence.lastDay.isBefore(from) }
            .toList()
    }

    /** Lazily generated start dates of the series honouring until/count (count includes excluded dates). */
    fun startDates(event: Event): Sequence<LocalDate> {
        val rule = event.recurrence ?: return sequenceOf(event.startDate)
        var dates = candidates(event.startDate, rule).filter { !it.isBefore(event.startDate) }
        rule.until?.let { until -> dates = dates.takeWhile { !it.isAfter(until) } }
        rule.count?.let { count -> dates = dates.take(count) }
        return dates.take(MAX_OCCURRENCES)
    }

    private fun candidates(start: LocalDate, rule: Recurrence): Sequence<LocalDate> = when (rule.frequency) {
        Frequency.DAILY -> generateSequence(0L) { it + rule.interval }.map { start.plusDays(it) }

        Frequency.WEEKLY -> {
            val days = rule.daysOfWeek.ifEmpty { setOf(start.dayOfWeek) }.sortedBy { it.value }
            val firstWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            generateSequence(0L) { it + rule.interval }.flatMap { weekOffset ->
                val weekStart = firstWeek.plusWeeks(weekOffset)
                days.asSequence().map { weekStart.plusDays((it.value - 1).toLong()) }
            }
        }

        Frequency.MONTHLY -> generateSequence(0L) { it + rule.interval }.mapNotNull { offset ->
            val month = java.time.YearMonth.from(start).plusMonths(offset)
            if (start.dayOfMonth <= month.lengthOfMonth()) month.atDay(start.dayOfMonth) else null
        }

        Frequency.YEARLY -> generateSequence(0L) { it + rule.interval }.mapNotNull { offset ->
            val year = start.year + offset.toInt()
            val month = java.time.YearMonth.of(year, start.month)
            if (start.dayOfMonth <= month.lengthOfMonth()) month.atDay(start.dayOfMonth) else null
        }

        Frequency.LUNAR_MONTHLY -> {
            val lunarStart = lunarCalendar.toLunar(start)
            lunarMonthsFrom(lunarStart.year)
                .dropWhile { it.startJdn + it.length <= jdnOf(start) }
                .filterIndexed { index, _ -> index % rule.interval == 0 }
                .map { month -> LunarCalendar.jdnToDate(month.startJdn + minOf(lunarStart.day, month.length) - 1) }
        }

        Frequency.LUNAR_YEARLY -> {
            val lunarStart = lunarCalendar.toLunar(start)
            generateSequence(0) { it + rule.interval }.mapNotNull { offset ->
                // A date first set in a leap month recurs in the regular month of the same number;
                // day 30 in a 29-day month is observed on the 29th (tháng thiếu).
                val month = lunarCalendar.findMonth(lunarStart.month, lunarStart.year + offset) ?: return@mapNotNull null
                LunarCalendar.jdnToDate(month.startJdn + minOf(lunarStart.day, month.length) - 1)
            }
        }
    }

    private fun lunarMonthsFrom(lunarYear: Int) =
        generateSequence(lunarYear) { it + 1 }.flatMap { lunarCalendar.monthsOfYear(it).asSequence() }

    private fun jdnOf(date: LocalDate): Int = with(LunarCalendar) { date.toJdn() }

    private companion object {
        /** Hard stop against runaway series (≈ 27 years of daily events). */
        const val MAX_OCCURRENCES = 10_000
    }
}
