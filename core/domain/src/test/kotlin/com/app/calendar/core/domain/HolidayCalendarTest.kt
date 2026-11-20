package com.app.calendar.core.domain

import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.HolidayRule
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HolidayCalendarTest {
    private val lunar = LunarCalendar()
    private val rules = listOf(
        HolidayRule("tet-duong-lich", "Tết Dương lịch", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 1, 1),
        HolidayRule("tet-nguyen-dan", "Tết Nguyên Đán", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.LUNAR, 1, 1, durationDays = 3),
        HolidayRule("giao-thua", "Giao thừa", HolidayCategory.TRADITIONAL, CalendarSystem.LUNAR, 12, 0, lastDayOfMonth = true),
        HolidayRule("ong-cong-ong-tao", "Ông Công Ông Táo", HolidayCategory.TRADITIONAL, CalendarSystem.LUNAR, 12, 23),
        HolidayRule("gio-to", "Giỗ Tổ Hùng Vương", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.LUNAR, 3, 10, sinceYear = 2007),
        HolidayRule("trung-thu", "Tết Trung Thu", HolidayCategory.TRADITIONAL, CalendarSystem.LUNAR, 8, 15),
    )

    private fun resolve(year: Int) = HolidayCalendar.resolve(rules, year, lunar)

    @Test
    fun `tet spans three days on the correct dates`() {
        val tet = resolve(2026).filter { it.ruleId == "tet-nguyen-dan" }
        assertThat(tet.map { it.date }).containsExactly(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18), LocalDate.of(2026, 2, 19)).inOrder()
        assertThat(tet.map { it.dayIndex }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `new year's eve is the last day of the twelfth lunar month, the day before tet`() {
        for (year in 2020..2035) {
            val tet = lunar.toSolar(1, 1, year)!!
            val eve = resolve(year).single { it.ruleId == "giao-thua" }
            assertThat(eve.date).isEqualTo(tet.minusDays(1))
        }
    }

    @Test
    fun `lunar holidays from the previous lunar year are included when they fall in january`() {
        // 23 tháng Chạp of lunar 2024 falls on 2025-01-22.
        val ongTao = resolve(2025).filter { it.ruleId == "ong-cong-ong-tao" }
        assertThat(ongTao.map { it.date }).contains(LocalDate.of(2025, 1, 22))
    }

    @Test
    fun `lunar holidays are recomputed every year, never fixed`() {
        val trungThu = (2024..2027).map { year -> resolve(year).single { it.ruleId == "trung-thu" }.date }
        assertThat(trungThu.map { it.dayOfYear }.toSet().size).isGreaterThan(1)
        trungThu.forEach { assertThat(lunar.toLunar(it).let { l -> l.day to l.month }).isEqualTo(15 to 8) }
    }

    @Test
    fun `since year is respected`() {
        assertThat(resolve(2006).none { it.ruleId == "gio-to" }).isTrue()
        assertThat(resolve(2007).any { it.ruleId == "gio-to" }).isTrue()
    }
}
