package com.app.calendar.core.domain.holiday

import com.app.calendar.core.domain.repository.HolidayRepository
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialDayOff
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class YearHolidays(
    val year: Int,
    val holidays: List<Holiday>,
    val daysOff: Map<LocalDate, OfficialDayOff>,
) {
    private val byDate: Map<LocalDate, List<Holiday>> = holidays.groupBy { it.date }
    fun holidaysOn(date: LocalDate): List<Holiday> = byDate[date].orEmpty()
}

/** Resolves holiday rules to concrete dates per year and caches the result. */
@Singleton
class HolidayCalendar @Inject constructor(
    private val repository: HolidayRepository,
    private val lunarCalendar: LunarCalendar,
) {
    private val mutex = Mutex()
    private val cache = HashMap<Int, YearHolidays>()

    suspend fun year(year: Int): YearHolidays {
        mutex.withLock { cache[year]?.let { return it } }
        val resolved = YearHolidays(
            year = year,
            holidays = resolve(repository.rules(), year, lunarCalendar),
            daysOff = repository.officialDaysOff(year).associateBy { it.date },
        )
        mutex.withLock { cache[year] = resolved }
        return resolved
    }

    companion object {
        /** Pure resolution of [rules] into dates falling in Gregorian [year]. */
        fun resolve(rules: List<HolidayRule>, year: Int, lunarCalendar: LunarCalendar): List<Holiday> =
            rules.flatMap { rule ->
                val firstDays = when (rule.calendar) {
                    CalendarSystem.SOLAR -> listOfNotNull(solarDate(year, rule.month, rule.day))
                    // Lunar dates early in a Gregorian year may belong to the previous lunar year.
                    CalendarSystem.LUNAR -> listOf(year - 1, year).mapNotNull { lunarYear -> lunarDate(rule, lunarYear, lunarCalendar) }
                }
                firstDays.flatMap { first ->
                    (0 until rule.durationDays).map { offset ->
                        Holiday(
                            ruleId = rule.id,
                            name = rule.name,
                            category = rule.category,
                            date = first.plusDays(offset.toLong()),
                            description = rule.description,
                            calendar = rule.calendar,
                            dayIndex = offset + 1,
                        )
                    }
                }
            }
                .filter { it.date.year == year }
                .filter { holiday -> rules.first { it.id == holiday.ruleId }.sinceYear?.let { year >= it } ?: true }
                .sortedWith(compareBy({ it.date }, { it.category.ordinal }))

        private fun solarDate(year: Int, month: Int, day: Int): LocalDate? =
            runCatching { LocalDate.of(year, month, day) }.getOrNull()

        private fun lunarDate(rule: HolidayRule, lunarYear: Int, lunarCalendar: LunarCalendar): LocalDate? {
            val month = lunarCalendar.findMonth(rule.month, lunarYear) ?: return null
            val day = if (rule.lastDayOfMonth) month.length else rule.day
            if (day > month.length) return null
            return LunarCalendar.jdnToDate(month.startJdn + day - 1)
        }
    }
}
