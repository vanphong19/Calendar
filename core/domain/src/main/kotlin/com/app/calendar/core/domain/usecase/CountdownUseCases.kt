package com.app.calendar.core.domain.usecase

import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.model.Holiday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Days remaining until [holiday] (0 = today). */
data class HolidayCountdown(val holiday: Holiday, val daysLeft: Long)

/** The first day of every holiday within the next year, each holiday listed once. */
class UpcomingHolidaysUseCase @Inject constructor(private val holidayCalendar: HolidayCalendar) {
    suspend operator fun invoke(today: LocalDate, days: Long = 365): List<Holiday> {
        val end = today.plusDays(days)
        return (today.year..end.year)
            .flatMap { holidayCalendar.year(it).holidays }
            .filter { it.dayIndex == 1 && !it.date.isBefore(today) && !it.date.isAfter(end) }
            .sortedBy { it.date }
            .distinctBy { it.ruleId }
    }
}

/** Countdown to the holiday the user picked (default: Tết Nguyên Đán), within the next year. */
class ObserveHolidayCountdownUseCase @Inject constructor(
    private val preferences: PreferencesRepository,
    private val upcomingHolidays: UpcomingHolidaysUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(today: LocalDate): Flow<HolidayCountdown?> =
        preferences.preferences.map { it.countdownHolidayId ?: DEFAULT_HOLIDAY }.distinctUntilChanged().mapLatest { id ->
            val upcoming = upcomingHolidays(today)
            val target = upcoming.firstOrNull { it.ruleId == id } ?: upcoming.firstOrNull { it.ruleId == DEFAULT_HOLIDAY }
            target?.let { HolidayCountdown(it, ChronoUnit.DAYS.between(today, it.date)) }
        }

    companion object {
        const val DEFAULT_HOLIDAY = "tet-nguyen-dan"
    }
}
