package com.app.calendar.feature.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.calendar.core.designsystem.theme.LichTheme
import com.app.calendar.core.domain.calendar.CalendarGrid
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.DayCell
import com.app.calendar.core.model.DayDetails
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.feature.calendar.month.MonthGrid
import com.app.calendar.feature.calendar.today.TearOffPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarScreensTest {
    @get:Rule val compose = createComposeRule()
    private val lunar = LunarCalendar()

    private fun details(date: LocalDate, holidays: List<Holiday> = emptyList()): DayDetails {
        val l = lunar.toLunar(date)
        return DayDetails(
            date = date, lunar = l,
            dayCanChi = lunar.dayCanChi(date), monthCanChi = lunar.monthCanChi(l.month, l.year), yearCanChi = lunar.yearCanChi(l.year),
            solarTerm = lunar.solarTerm(date), isSolarTermStart = false, auspiciousHours = lunar.auspiciousHours(date),
            holidays = holidays, dayOff = null, occurrences = emptyList(),
        )
    }

    @Test
    fun tearOffPageShowsSolarLunarAndHoliday() {
        val date = LocalDate.of(2026, 11, 20)
        val teachers = Holiday("nha-giao", "Ngày Nhà giáo Việt Nam", HolidayCategory.OBSERVANCE, date, "", CalendarSystem.SOLAR)
        compose.setContent {
            LichTheme {
                TearOffPage(DayPageState(details(date, listOf(teachers)), null), PageSettings(showImages = false), isToday = true, onOpenDetails = {})
            }
        }
        compose.onNodeWithContentDescription("Thứ Sáu, 20/11/2026, hôm nay").assertIsDisplayed()
        compose.onNodeWithText("NGÀY NHÀ GIÁO VIỆT NAM").assertIsDisplayed()
        compose.onNodeWithText("Ngày Mậu Tuất").assertIsDisplayed()
        compose.onNodeWithText("2026").assertIsDisplayed()
    }

    @Test
    fun lunarBoxIsHiddenWhenDisabled() {
        compose.setContent {
            LichTheme {
                TearOffPage(DayPageState(details(LocalDate.of(2026, 11, 20)), null), PageSettings(showLunar = false, showImages = false), isToday = false, onOpenDetails = {})
            }
        }
        compose.onNodeWithText("ÂM LỊCH").assertDoesNotExist()
    }

    @Test
    fun monthGridSelectsTappedDay() {
        val month = YearMonth.of(2026, 11)
        val cells = CalendarGrid.monthDates(month, DayOfWeek.MONDAY).map { date ->
            DayCell(date, lunar.toLunar(date), YearMonth.from(date) == month, emptyList(), null,
                if (date.dayOfMonth == 21) listOf(EventColor.RIVER) else emptyList(), if (date.dayOfMonth == 21) 1 else 0)
        }
        var selected = LocalDate.of(2026, 11, 20)
        compose.setContent {
            LichTheme {
                MonthGrid(cells, today = LocalDate.of(2026, 11, 20), selected = selected, showLunar = true, onSelect = { selected = it })
            }
        }
        compose.onNodeWithContentDescription("Thứ Sáu, 20/11/2026", substring = true).assertIsSelected()
        compose.onNodeWithContentDescription("Thứ Bảy, 21/11/2026, 13/10, 1 sự kiện", substring = true).performClick()
        assertThat(selected).isEqualTo(LocalDate.of(2026, 11, 21))
    }
}
