package com.app.calendar.core.domain

import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.repository.HolidayRepository
import com.app.calendar.core.domain.usecase.UpcomingHolidaysUseCase
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialDayOff
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class CountdownTest {
    private val repo = object : HolidayRepository {
        override suspend fun rules() = listOf(
            HolidayRule("tet-nguyen-dan", "Tết Nguyên Đán", HolidayCategory.PUBLIC_HOLIDAY, CalendarSystem.LUNAR, 1, 1, durationDays = 3),
            HolidayRule("nha-giao", "Ngày Nhà giáo Việt Nam", HolidayCategory.OBSERVANCE, CalendarSystem.SOLAR, 11, 20),
        )
        override suspend fun officialDaysOff(year: Int) = emptyList<OfficialDayOff>()
    }
    private val upcoming = UpcomingHolidaysUseCase(HolidayCalendar(repo, LunarCalendar()))

    @Test
    fun `lists each holiday once within the next year, crossing into the next year`() = runTest {
        val list = upcoming(LocalDate.of(2026, 11, 21))
        assertThat(list.map { it.ruleId to it.date }).containsExactly(
            "tet-nguyen-dan" to LocalDate.of(2027, 2, 6),
            "nha-giao" to LocalDate.of(2027, 11, 20),
        ).inOrder()
    }

    @Test
    fun `a holiday today is included`() = runTest {
        assertThat(upcoming(LocalDate.of(2026, 11, 20)).first().date).isEqualTo(LocalDate.of(2026, 11, 20))
    }
}
