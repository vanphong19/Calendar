package com.app.calendar.core.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class HolidayTest {
    @Test
    fun `holiday image is used on the first day only`() {
        val holiday = Holiday("tet", "Tết", HolidayCategory.PUBLIC_HOLIDAY, LocalDate.of(2026, 2, 17), "", CalendarSystem.LUNAR)
        assertThat(listOf(holiday).dailyContentRuleIds()).containsExactly("tet")
        assertThat(listOf(holiday.copy(date = holiday.date.plusDays(1), dayIndex = 2)).dailyContentRuleIds()).isEmpty()
    }
}
