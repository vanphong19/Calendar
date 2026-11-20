package com.app.calendar.core.domain.usecase

import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.holiday.YearHolidays
import com.app.calendar.core.domain.recurrence.RecurrenceExpander
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.DayCell
import com.app.calendar.core.model.DayDetails
import com.app.calendar.core.model.Event
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Shared helper: holidays visible under the current preferences, for every year a range touches. */
class VisibleHolidays @Inject constructor(private val holidayCalendar: HolidayCalendar) {
    suspend fun forRange(from: LocalDate, to: LocalDate, prefs: UserPreferences): Map<Int, YearHolidays> =
        (from.year..to.year).associateWith { year ->
            val all = holidayCalendar.year(year)
            all.copy(holidays = all.holidays.filter { prefs.isHolidayVisible(it.category) })
        }
}

private fun Map<Int, YearHolidays>.on(date: LocalDate): List<Holiday> = get(date.year)?.holidaysOn(date).orEmpty()

private fun List<Event>.occurrences(expander: RecurrenceExpander, from: LocalDate, to: LocalDate): List<EventOccurrence> =
    flatMap { expander.expand(it, from, to) }
        .sortedWith(compareBy<EventOccurrence>({ !it.event.isAllDay }, { it.start }, { it.event.title }))

/** Per-day cells for an arbitrary list of dates (month or week grid). */
class ObserveDayCellsUseCase @Inject constructor(
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val visibleHolidays: VisibleHolidays,
    private val expander: RecurrenceExpander,
    private val lunarCalendar: LunarCalendar,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(dates: List<LocalDate>, focusMonth: YearMonth?): Flow<List<DayCell>> {
        val from = dates.first()
        val to = dates.last()
        return combine(events.observeEventsTouching(from, to), preferences.preferences) { list, prefs -> list to prefs }
            .mapLatest { (list, prefs) ->
                val holidays = visibleHolidays.forRange(from, to, prefs)
                val occurrences = list.occurrences(expander, from, to)
                dates.map { date ->
                    val onDay = occurrences.filter { it.covers(date) }
                    DayCell(
                        date = date,
                        lunar = lunarCalendar.toLunar(date),
                        isInCurrentMonth = focusMonth == null || YearMonth.from(date) == focusMonth,
                        holidays = holidays.on(date),
                        dayOff = holidays[date.year]?.daysOff?.get(date)?.takeIf { prefs.showHolidays },
                        eventColors = onDay.map { it.event.color }.distinct().take(3),
                        eventCount = onDay.size,
                    )
                }
            }
    }
}

/** Everything for one tear-off page. */
class ObserveDayDetailsUseCase @Inject constructor(
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val visibleHolidays: VisibleHolidays,
    private val expander: RecurrenceExpander,
    private val lunarCalendar: LunarCalendar,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<DayDetails> =
        combine(events.observeEventsTouching(date, date), preferences.preferences) { list, prefs -> list to prefs }
            .mapLatest { (list, prefs) ->
                val holidays = visibleHolidays.forRange(date, date, prefs)
                buildDetails(date, holidays, list.occurrences(expander, date, date), prefs)
            }

    fun lunarOnly(date: LocalDate): DayDetails = buildDetails(date, emptyMap(), emptyList(), UserPreferences())

    private fun buildDetails(
        date: LocalDate,
        holidays: Map<Int, YearHolidays>,
        occurrences: List<EventOccurrence>,
        prefs: UserPreferences,
    ): DayDetails {
        val lunar = lunarCalendar.toLunar(date)
        return DayDetails(
            date = date,
            lunar = lunar,
            dayCanChi = lunarCalendar.dayCanChi(date),
            monthCanChi = lunarCalendar.monthCanChi(lunar.month, lunar.year),
            yearCanChi = lunarCalendar.yearCanChi(lunar.year),
            solarTerm = lunarCalendar.solarTerm(date),
            isSolarTermStart = lunarCalendar.isSolarTermStart(date),
            auspiciousHours = lunarCalendar.auspiciousHours(date),
            holidays = holidays.on(date),
            dayOff = holidays[date.year]?.daysOff?.get(date)?.takeIf { prefs.showHolidays },
            occurrences = occurrences.filter { it.covers(date) },
        )
    }
}

data class AgendaDay(val date: LocalDate, val holidays: List<Holiday>, val occurrences: List<EventOccurrence>)

/** Chronological list of days that have events or holidays (lịch trình). */
class ObserveAgendaUseCase @Inject constructor(
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val visibleHolidays: VisibleHolidays,
    private val expander: RecurrenceExpander,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<AgendaDay>> =
        combine(events.observeEventsTouching(from, to), preferences.preferences) { list, prefs -> list to prefs }
            .mapLatest { (list, prefs) ->
                val holidays = visibleHolidays.forRange(from, to, prefs)
                val occurrences = list.occurrences(expander, from, to)
                generateSequence(from) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(to) }
                    .map { date -> AgendaDay(date, holidays.on(date), occurrences.filter { it.covers(date) }) }
                    .filter { it.holidays.isNotEmpty() || it.occurrences.isNotEmpty() }
                    .toList()
            }
}

/** Holidays of one Gregorian year, honouring the visibility preferences. */
class ObserveYearHolidaysUseCase @Inject constructor(
    private val preferences: PreferencesRepository,
    private val holidayCalendar: HolidayCalendar,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(year: Int): Flow<YearHolidays> =
        preferences.preferences.map { it.showHolidays }.distinctUntilChanged().mapLatest { holidayCalendar.year(year) }
}
