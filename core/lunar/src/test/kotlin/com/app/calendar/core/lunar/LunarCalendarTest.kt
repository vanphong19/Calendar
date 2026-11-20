package com.app.calendar.core.lunar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class LunarCalendarTest {

    private val calendar = LunarCalendar()

    @Test
    fun `tet dates match the official Vietnamese calendar`() {
        val tet = mapOf(
            1985 to "1985-01-21", // differs from the Chinese calendar (UTC+7 vs UTC+8)
            2000 to "2000-02-05", 2001 to "2001-01-24", 2002 to "2002-02-12", 2003 to "2003-02-01",
            2004 to "2004-01-22", 2005 to "2005-02-09", 2006 to "2006-01-29",
            2007 to "2007-02-17", // differs from the Chinese calendar
            2008 to "2008-02-07", 2009 to "2009-01-26", 2010 to "2010-02-14", 2011 to "2011-02-03",
            2012 to "2012-01-23", 2013 to "2013-02-10", 2014 to "2014-01-31", 2015 to "2015-02-19",
            2016 to "2016-02-08", 2017 to "2017-01-28", 2018 to "2018-02-16", 2019 to "2019-02-05",
            2020 to "2020-01-25", 2021 to "2021-02-12", 2022 to "2022-02-01", 2023 to "2023-01-22",
            2024 to "2024-02-10", 2025 to "2025-01-29", 2026 to "2026-02-17", 2027 to "2027-02-06",
            2028 to "2028-01-26", 2029 to "2029-02-13",
            2030 to "2030-02-02", // new moon 22:07 UTC+7 on 2 Feb; China (UTC+8) celebrates on 3 Feb
        )
        tet.forEach { (year, date) ->
            val expected = LocalDate.parse(date)
            assertThat(calendar.toSolar(1, 1, year)).isEqualTo(expected)
            assertThat(calendar.toLunar(expected)).isEqualTo(
                LunarDate(day = 1, month = 1, year = year, isLeapMonth = false, monthLength = calendar.findMonth(1, year)!!.length),
            )
        }
    }

    @Test
    fun `leap months are placed correctly`() {
        mapOf(2012 to 4, 2014 to 9, 2017 to 6, 2020 to 4, 2023 to 2, 2025 to 6).forEach { (year, leap) ->
            assertThat(calendar.leapMonthOf(year)).isEqualTo(leap)
            assertThat(calendar.monthsOfYear(year)).hasSize(13)
        }
        listOf(2018, 2019, 2021, 2022, 2024, 2026).forEach { year ->
            assertThat(calendar.leapMonthOf(year)).isNull()
            assertThat(calendar.monthsOfYear(year)).hasSize(12)
        }
    }

    @Test
    fun `leap month dates convert both ways`() {
        // 2023 had a leap second month: 1/2 nhuận = 2023-03-22.
        val leapStart = calendar.toSolar(1, 2, 2023, isLeapMonth = true)
        assertThat(leapStart).isEqualTo(LocalDate.of(2023, 3, 22))
        assertThat(calendar.toLunar(LocalDate.of(2023, 3, 22)).isLeapMonth).isTrue()
        assertThat(calendar.toSolar(1, 3, 2023, isLeapMonth = true)).isNull()
    }

    @Test
    fun `every day from 1900 to 2100 round-trips and advances by one`() {
        var date = LocalDate.of(1900, 1, 31)
        val end = LocalDate.of(2100, 12, 31)
        var previous = calendar.toLunar(date.minusDays(1))
        while (!date.isAfter(end)) {
            val lunar = calendar.toLunar(date)
            assertThat(calendar.toSolar(lunar)).isEqualTo(date)
            assertThat(lunar.monthLength).isAnyOf(29, 30)
            if (lunar.day == 1) {
                assertThat(previous.day).isEqualTo(previous.monthLength)
            } else {
                assertThat(lunar.day).isEqualTo(previous.day + 1)
                assertThat(lunar.month).isEqualTo(previous.month)
                assertThat(lunar.year).isEqualTo(previous.year)
            }
            previous = lunar
            date = date.plusDays(1)
        }
    }

    @Test
    fun `every lunar year has 12 or 13 months in order with at most one leap`() {
        for (year in 1901..2099) {
            val months = calendar.monthsOfYear(year)
            assertThat(months.size).isAnyOf(12, 13)
            assertThat(months.count { it.isLeap }).isEqualTo(months.size - 12)
            assertThat(months.filterNot { it.isLeap }.map { it.month }).isEqualTo((1..12).toList())
            months.zipWithNext().forEach { (a, b) -> assertThat(b.startJdn).isEqualTo(a.startJdn + a.length) }
        }
    }

    @Test
    fun `non existent dates return null`() {
        val shortMonth = calendar.monthsOfYear(2026).first { it.length == 29 }
        assertThat(calendar.toSolar(30, shortMonth.month, 2026, shortMonth.isLeap)).isNull()
        assertThat(calendar.toSolar(1, 5, 2026, isLeapMonth = true)).isNull()
    }

    @Test
    fun `can chi of years months and days`() {
        assertThat(calendar.yearCanChi(2026).displayName).isEqualTo("Bính Ngọ")
        assertThat(calendar.yearCanChi(2025).displayName).isEqualTo("Ất Tỵ")
        assertThat(calendar.yearCanChi(2024).displayName).isEqualTo("Giáp Thìn")
        assertThat(calendar.yearCanChi(1945).displayName).isEqualTo("Ất Dậu")
        assertThat(calendar.dayCanChi(LocalDate.of(2000, 1, 1)).displayName).isEqualTo("Mậu Ngọ")
        // Tháng Giêng of a Giáp/Kỷ year is Bính Dần.
        assertThat(calendar.monthCanChi(1, 2024).displayName).isEqualTo("Bính Dần")
        assertThat(calendar.monthCanChi(1, 2029).displayName).isEqualTo("Bính Dần")
    }

    @Test
    fun `auspicious hours always contain six branches`() {
        var date = LocalDate.of(2026, 1, 1)
        repeat(24) {
            assertThat(calendar.auspiciousHours(date)).hasSize(6)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `solar terms fall on expected days`() {
        assertThat(calendar.solarTerm(LocalDate.of(2026, 6, 21))).isEqualTo(SolarTerm.HA_CHI)
        assertThat(calendar.solarTerm(LocalDate.of(2026, 12, 22))).isEqualTo(SolarTerm.DONG_CHI)
        assertThat(calendar.solarTerm(LocalDate.of(2026, 3, 21))).isEqualTo(SolarTerm.XUAN_PHAN)
        assertThat(calendar.isSolarTermStart(LocalDate.of(2026, 2, 4))).isTrue() // Lập xuân
    }
}
