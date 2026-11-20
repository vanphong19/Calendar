package com.app.calendar.core.domain

import com.app.calendar.core.domain.recurrence.RecurrenceExpander
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceExpanderTest {
    private val lunar = LunarCalendar()
    private val expander = RecurrenceExpander(lunar)
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `single event only occurs on its own day`() {
        val event = timedEvent("2026-11-20T09:00", "2026-11-20T10:00")
        assertThat(expander.expand(event, d("2026-11-01"), d("2026-11-30")).map { it.date }).containsExactly(d("2026-11-20"))
        assertThat(expander.expand(event, d("2026-11-21"), d("2026-11-30"))).isEmpty()
    }

    @Test
    fun `multi day event touches range from before`() {
        val event = allDayEvent("2026-11-18", "2026-11-22")
        val result = expander.expand(event, d("2026-11-20"), d("2026-11-20"))
        assertThat(result).hasSize(1)
        assertThat(result.single().covers(d("2026-11-22"))).isTrue()
        assertThat(result.single().covers(d("2026-11-23"))).isFalse()
    }

    @Test
    fun `timed event ending at midnight does not spill into next day`() {
        val occurrence = expander.expand(timedEvent("2026-11-20T22:00", "2026-11-21T00:00"), d("2026-11-20"), d("2026-11-21")).single()
        assertThat(occurrence.covers(d("2026-11-21"))).isFalse()
    }

    @Test
    fun `daily with interval and count`() {
        val event = timedEvent("2026-01-01T08:00", "2026-01-01T09:00", Recurrence(Frequency.DAILY, interval = 2, count = 3))
        assertThat(expander.expand(event, d("2026-01-01"), d("2026-01-31")).map { it.date })
            .containsExactly(d("2026-01-01"), d("2026-01-03"), d("2026-01-05")).inOrder()
    }

    @Test
    fun `weekly on several weekdays until a date`() {
        val rule = Recurrence(Frequency.WEEKLY, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), until = d("2026-11-18"))
        val event = timedEvent("2026-11-04T18:00", "2026-11-04T19:00", rule) // Wednesday
        assertThat(expander.expand(event, d("2026-11-01"), d("2026-11-30")).map { it.date })
            .containsExactly(d("2026-11-04"), d("2026-11-09"), d("2026-11-11"), d("2026-11-16"), d("2026-11-18")).inOrder()
    }

    @Test
    fun `monthly on the 31st skips short months`() {
        val event = timedEvent("2026-01-31T08:00", "2026-01-31T09:00", Recurrence(Frequency.MONTHLY))
        assertThat(expander.expand(event, d("2026-01-01"), d("2026-07-31")).map { it.date })
            .containsExactly(d("2026-01-31"), d("2026-03-31"), d("2026-05-31"), d("2026-07-31")).inOrder()
    }

    @Test
    fun `yearly on 29 February only occurs in leap years`() {
        val event = allDayEvent("2024-02-29", recurrence = Recurrence(Frequency.YEARLY))
        assertThat(expander.expand(event, d("2024-01-01"), d("2032-12-31")).map { it.date })
            .containsExactly(d("2024-02-29"), d("2028-02-29"), d("2032-02-29")).inOrder()
    }

    @Test
    fun `excluded dates are skipped but still count`() {
        val event = timedEvent("2026-01-01T08:00", "2026-01-01T09:00", Recurrence(Frequency.DAILY, count = 3))
            .copy(excludedDates = setOf(d("2026-01-02")))
        assertThat(expander.expand(event, d("2026-01-01"), d("2026-01-31")).map { it.date })
            .containsExactly(d("2026-01-01"), d("2026-01-03")).inOrder()
    }

    @Test
    fun `lunar yearly follows the lunar date each year`() {
        // Giỗ on 10/3 âm lịch: Giỗ Tổ Hùng Vương dates.
        val start = lunar.toSolar(10, 3, 2024)!!
        val event = allDayEvent(start.toString(), recurrence = Recurrence(Frequency.LUNAR_YEARLY))
        val dates = expander.expand(event, d("2024-01-01"), d("2027-12-31")).map { it.date }
        assertThat(dates).containsExactly(
            lunar.toSolar(10, 3, 2024), lunar.toSolar(10, 3, 2025), lunar.toSolar(10, 3, 2026), lunar.toSolar(10, 3, 2027),
        ).inOrder()
        assertThat(dates.map { lunar.toLunar(it).day }).containsExactly(10, 10, 10, 10)
    }

    @Test
    fun `lunar yearly on day 30 falls back to the 29th in short months`() {
        val longMonthYear = (2020..2040).first { lunar.findMonth(12, it)!!.length == 30 }
        val start = lunar.toSolar(30, 12, longMonthYear)!!
        val event = allDayEvent(start.toString(), recurrence = Recurrence(Frequency.LUNAR_YEARLY))
        expander.expand(event, start, start.plusYears(10)).forEach { occurrence ->
            val l = lunar.toLunar(occurrence.date)
            assertThat(l.month).isEqualTo(12)
            assertThat(l.day).isEqualTo(l.monthLength)
        }
    }

    @Test
    fun `lunar monthly on the full moon includes leap months`() {
        val start = lunar.toSolar(15, 1, 2025)!!
        val event = allDayEvent(start.toString(), recurrence = Recurrence(Frequency.LUNAR_MONTHLY))
        val dates = expander.expand(event, d("2025-01-01"), d("2025-12-31")).map { it.date }
        assertThat(dates.map { lunar.toLunar(it).day }.toSet()).containsExactly(15)
        assertThat(dates.any { lunar.toLunar(it).isLeapMonth }).isTrue() // 2025 has leap month 6
    }
}
