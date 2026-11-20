package com.app.calendar.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.app.calendar.core.designsystem.R
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.HolidayCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Vietnamese labels backed by string resources. */
object LichFormat {
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Composable @ReadOnlyComposable
    fun weekdayFull(day: DayOfWeek): String = stringArrayResource(R.array.weekday_full)[day.value - 1]

    @Composable @ReadOnlyComposable
    fun weekdayShort(day: DayOfWeek): String = stringArrayResource(R.array.weekday_short)[day.value - 1]

    @Composable @ReadOnlyComposable
    fun monthName(month: Int): String = stringArrayResource(R.array.month_names)[month - 1]

    @Composable @ReadOnlyComposable
    fun lunarMonthName(month: Int): String = stringArrayResource(R.array.lunar_month_names)[month - 1]

    @Composable @ReadOnlyComposable
    fun monthYear(month: Int, year: Int): String = stringResource(R.string.month_year, month, year)

    @Composable @ReadOnlyComposable
    fun solarDate(date: LocalDate): String = stringResource(R.string.solar_date, date.dayOfMonth, date.monthValue, date.year)

    /** "Tháng Mười" / "Tháng Sáu nhuận". */
    @Composable @ReadOnlyComposable
    fun lunarMonth(lunar: LunarDate): String = stringResource(
        if (lunar.isLeapMonth) R.string.lunar_month_leap else R.string.lunar_month,
        lunarMonthName(lunar.month),
    )

    /** Grid label: "1/10" on the first day of a lunar month, otherwise the day number. */
    @Composable @ReadOnlyComposable
    fun lunarGridLabel(lunar: LunarDate): String =
        if (lunar.day == 1) stringResource(R.string.lunar_day_month, lunar.day, lunar.month) else lunar.day.toString()

    @Composable @ReadOnlyComposable
    fun lunarDateLong(lunar: LunarDate, yearName: String): String = stringResource(
        if (lunar.isLeapMonth) R.string.lunar_date_long_leap else R.string.lunar_date_long,
        lunar.day,
        lunarMonthName(lunar.month),
        yearName,
    )

    fun time(time: LocalTime): String = time.format(TIME)

    @Composable @ReadOnlyComposable
    fun occurrenceTime(occurrence: EventOccurrence): String =
        if (occurrence.event.isAllDay) {
            stringResource(R.string.all_day)
        } else {
            stringResource(R.string.time_range, time(occurrence.start.toLocalTime()), time(occurrence.end.toLocalTime()))
        }

    @Composable @ReadOnlyComposable
    fun eventColorName(color: EventColor): String = stringResource(
        when (color) {
            EventColor.LACQUER -> R.string.event_color_lacquer
            EventColor.RICE -> R.string.event_color_rice
            EventColor.APRICOT -> R.string.event_color_apricot
            EventColor.RIVER -> R.string.event_color_river
            EventColor.LOTUS -> R.string.event_color_lotus
            EventColor.INK -> R.string.event_color_ink
        },
    )

    @Composable @ReadOnlyComposable
    fun holidayCategory(category: HolidayCategory): String = stringResource(
        when (category) {
            HolidayCategory.PUBLIC_HOLIDAY -> R.string.holiday_category_public
            HolidayCategory.TRADITIONAL -> R.string.holiday_category_traditional
            HolidayCategory.OBSERVANCE -> R.string.holiday_category_observance
        },
    )

    /** "Trước 15 phút", "Trước 1 giờ"…; for all-day events "17:00, 1 ngày trước". */
    @Composable @ReadOnlyComposable
    fun reminder(minutesBefore: Int?, isAllDay: Boolean): String {
        if (minutesBefore == null) return stringResource(R.string.reminder_none)
        if (isAllDay) {
            val daysBefore = Math.floorDiv(minutesBefore + 24 * 60 - 1, 24 * 60).coerceAtLeast(0)
            val clock = LocalTime.MIDNIGHT.minusMinutes(minutesBefore.toLong())
            return if (daysBefore == 0) {
                stringResource(R.string.reminder_all_day_same_day, time(clock))
            } else {
                stringResource(R.string.reminder_all_day_days_before, time(clock), daysBefore)
            }
        }
        return when {
            minutesBefore == 0 -> stringResource(R.string.reminder_at_start)
            minutesBefore % (24 * 60) == 0 -> stringResource(R.string.reminder_days, minutesBefore / (24 * 60))
            minutesBefore % 60 == 0 -> stringResource(R.string.reminder_hours, minutesBefore / 60)
            else -> stringResource(R.string.reminder_minutes, minutesBefore)
        }
    }

    /** Non-composable access for places like semantics builders. */
    @Composable
    fun context() = LocalContext.current
}
