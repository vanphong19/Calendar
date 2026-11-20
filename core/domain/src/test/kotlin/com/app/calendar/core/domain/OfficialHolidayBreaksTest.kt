package com.app.calendar.core.domain

import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.holiday.YearHolidays
import com.app.calendar.core.domain.holiday.breaksForHoliday
import com.app.calendar.core.domain.holiday.buildOfficialBreaks
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialDayOff
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class OfficialHolidayBreaksTest {
    private val lunar = LunarCalendar()
    private val statutoryRules = listOf(
        HolidayRule("tet-duong-lich", "Tết Dương lịch", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 1, 1),
        HolidayRule("tet-nguyen-dan", "Tết Nguyên Đán", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.LUNAR, 1, 1, durationDays = 3),
        HolidayRule("gio-to-hung-vuong", "Giỗ Tổ Hùng Vương", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.LUNAR, 3, 10, sinceYear = 2007),
        HolidayRule("giai-phong-mien-nam", "Ngày Giải phóng miền Nam, thống nhất đất nước", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 4, 30),
        HolidayRule("quoc-te-lao-dong", "Ngày Quốc tế Lao động", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 5, 1),
        HolidayRule("quoc-khanh", "Quốc khánh", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 9, 2),
        HolidayRule("van-hoa-viet-nam", "Ngày Văn hóa Việt Nam", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.SOLAR, 11, 24, sinceYear = 2026),
        HolidayRule("valentine", "Lễ Tình nhân", HolidayCategory.OBSERVANCE, CalendarSystem.SOLAR, 2, 14),
    )

    @Test
    fun `announced days off group consecutive dates and attach matching make-up workdays`() {
        val holidays = HolidayCalendar.resolve(statutoryRules, 2026, lunar)
        val daysOff = listOf(
            OfficialDayOff(LocalDate.of(2026, 1, 1), OfficialDayOff.Kind.DAY_OFF, "1 ngày lễ, 1 ngày hoán đổi và 2 ngày nghỉ hằng tuần", "12590/TB-BNV", title = "Tết Dương lịch", holidayIds = listOf("tet-duong-lich")),
            OfficialDayOff(LocalDate.of(2026, 1, 2), OfficialDayOff.Kind.DAY_OFF, "1 ngày lễ, 1 ngày hoán đổi và 2 ngày nghỉ hằng tuần", "12590/TB-BNV", title = "Tết Dương lịch", holidayIds = listOf("tet-duong-lich")),
            OfficialDayOff(LocalDate.of(2026, 1, 3), OfficialDayOff.Kind.DAY_OFF, "1 ngày lễ, 1 ngày hoán đổi và 2 ngày nghỉ hằng tuần", "12590/TB-BNV", title = "Tết Dương lịch", holidayIds = listOf("tet-duong-lich")),
            OfficialDayOff(LocalDate.of(2026, 1, 4), OfficialDayOff.Kind.DAY_OFF, "1 ngày lễ, 1 ngày hoán đổi và 2 ngày nghỉ hằng tuần", "12590/TB-BNV", title = "Tết Dương lịch", holidayIds = listOf("tet-duong-lich")),
            OfficialDayOff(LocalDate.of(2026, 1, 10), OfficialDayOff.Kind.MAKE_UP_WORKDAY, "Làm bù cho thứ Sáu 02/01", "12590/TB-BNV", title = "Tết Dương lịch", holidayIds = listOf("tet-duong-lich")),
            OfficialDayOff(LocalDate.of(2026, 4, 25), OfficialDayOff.Kind.DAY_OFF, "Nghỉ lễ và nghỉ bù Giỗ Tổ", "3383/BNV-CVL", title = "Giỗ Tổ Hùng Vương", holidayIds = listOf("gio-to-hung-vuong")),
            OfficialDayOff(LocalDate.of(2026, 4, 26), OfficialDayOff.Kind.DAY_OFF, "Nghỉ lễ và nghỉ bù Giỗ Tổ", "3383/BNV-CVL", title = "Giỗ Tổ Hùng Vương", holidayIds = listOf("gio-to-hung-vuong")),
            OfficialDayOff(LocalDate.of(2026, 4, 27), OfficialDayOff.Kind.DAY_OFF, "Nghỉ lễ và nghỉ bù Giỗ Tổ", "3383/BNV-CVL", title = "Giỗ Tổ Hùng Vương", holidayIds = listOf("gio-to-hung-vuong")),
            OfficialDayOff(LocalDate.of(2026, 4, 30), OfficialDayOff.Kind.DAY_OFF, "2 ngày lễ và 2 ngày cuối tuần", "BLLĐ 2019", title = "Ngày 30/4 & Quốc tế Lao động", holidayIds = listOf("giai-phong-mien-nam", "quoc-te-lao-dong")),
            OfficialDayOff(LocalDate.of(2026, 5, 1), OfficialDayOff.Kind.DAY_OFF, "2 ngày lễ và 2 ngày cuối tuần", "BLLĐ 2019", title = "Ngày 30/4 & Quốc tế Lao động", holidayIds = listOf("giai-phong-mien-nam", "quoc-te-lao-dong")),
            OfficialDayOff(LocalDate.of(2026, 5, 2), OfficialDayOff.Kind.DAY_OFF, "2 ngày lễ và 2 ngày cuối tuần", "BLLĐ 2019", title = "Ngày 30/4 & Quốc tế Lao động", holidayIds = listOf("giai-phong-mien-nam", "quoc-te-lao-dong")),
            OfficialDayOff(LocalDate.of(2026, 5, 3), OfficialDayOff.Kind.DAY_OFF, "2 ngày lễ và 2 ngày cuối tuần", "BLLĐ 2019", title = "Ngày 30/4 & Quốc tế Lao động", holidayIds = listOf("giai-phong-mien-nam", "quoc-te-lao-dong")),
        )
        val yearData = YearHolidays(2026, holidays, daysOff.associateBy { it.date })

        val breaks = buildOfficialBreaks(yearData)
        assertThat(breaks).hasSize(3)
        assertThat(breaks[0].title).isEqualTo("Tết Dương lịch")
        assertThat(breaks[0].dayCount).isEqualTo(4)
        assertThat(breaks[0].startDate).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(breaks[0].endDate).isEqualTo(LocalDate.of(2026, 1, 4))
        assertThat(breaks[0].makeUpWorkdays.map { it.date }).containsExactly(LocalDate.of(2026, 1, 10))
        assertThat(breaks[0].isAnnounced).isTrue()

        // Giỗ Tổ (25-27/4) must NOT leak into 30/4-1/5 (30/4-3/5) even though they are 4 days apart.
        val gioToBreaks = breaksForHoliday("gio-to-hung-vuong", yearData)
        assertThat(gioToBreaks).hasSize(1)
        assertThat(gioToBreaks.single().startDate).isEqualTo(LocalDate.of(2026, 4, 25))
        assertThat(gioToBreaks.single().endDate).isEqualTo(LocalDate.of(2026, 4, 27))

        val apr30Breaks = breaksForHoliday("giai-phong-mien-nam", yearData)
        assertThat(apr30Breaks).hasSize(1)
        assertThat(apr30Breaks.single().startDate).isEqualTo(LocalDate.of(2026, 4, 30))
        assertThat(apr30Breaks.single().endDate).isEqualTo(LocalDate.of(2026, 5, 3))

        // Tết Dương lịch detail includes its make-up workday even though 10/01 is 9 days after 01/01.
        val newYearBreaks = breaksForHoliday("tet-duong-lich", yearData)
        assertThat(newYearBreaks).hasSize(1)
        assertThat(newYearBreaks.single().makeUpWorkdays.map { it.date }).containsExactly(LocalDate.of(2026, 1, 10))
    }

    @Test
    fun `year without official announcement synthesizes statutory breaks and merges Apr 30 with May 1`() {
        val holidays = HolidayCalendar.resolve(statutoryRules, 2027, lunar)
        val yearData = YearHolidays(2027, holidays, emptyMap())

        val breaks = buildOfficialBreaks(yearData)
        assertThat(breaks).hasSize(6)
        assertThat(breaks.all { !it.isAnnounced }).isTrue()
        assertThat(breaks.map { it.holidayIds }).containsExactly(
            listOf("tet-duong-lich"),
            listOf("tet-nguyen-dan"),
            listOf("gio-to-hung-vuong"),
            listOf("giai-phong-mien-nam", "quoc-te-lao-dong"),
            listOf("quoc-khanh"),
            listOf("van-hoa-viet-nam"),
        ).inOrder()
        val combined = breaks[3]
        assertThat(combined.startDate).isEqualTo(LocalDate.of(2027, 4, 30))
        assertThat(combined.endDate).isEqualTo(LocalDate.of(2027, 5, 1))
        assertThat(combined.dayCount).isEqualTo(2)
        // Statutory display days reflect the Labor Code entitlement (5 for Tết, 2 for Quốc khánh).
        assertThat(breaks[1].displayDays).isEqualTo(5)
        assertThat(breaks[4].displayDays).isEqualTo(2)
        // No unbalanced quotes in synthesized statutory reasons.
        breaks.forEach { br -> assertThat(br.reason.count { it == '"' } % 2).isEqualTo(0) }
        // Non-public holiday Valentine is excluded.
        assertThat(breaks.flatMap { it.holidayIds }).doesNotContain("valentine")
    }
}
