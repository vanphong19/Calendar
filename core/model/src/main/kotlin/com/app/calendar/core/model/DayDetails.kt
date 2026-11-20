package com.app.calendar.core.model

import com.app.calendar.core.lunar.CanChi
import com.app.calendar.core.lunar.Chi
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.lunar.SolarTerm
import java.time.LocalDate

/** Everything printed on one tear-off calendar page. */
data class DayDetails(
    val date: LocalDate,
    val lunar: LunarDate,
    val dayCanChi: CanChi,
    val monthCanChi: CanChi,
    val yearCanChi: CanChi,
    val solarTerm: SolarTerm,
    val isSolarTermStart: Boolean,
    val auspiciousHours: List<Chi>,
    val holidays: List<Holiday>,
    val dayOff: OfficialDayOff?,
    val occurrences: List<EventOccurrence>,
)

/** Compact per-day data for month/week grids. */
data class DayCell(
    val date: LocalDate,
    val lunar: LunarDate,
    val isInCurrentMonth: Boolean,
    val holidays: List<Holiday>,
    val dayOff: OfficialDayOff?,
    /** Distinct colours of events on this day, at most three. */
    val eventColors: List<EventColor>,
    val eventCount: Int,
) {
    val isPublicHoliday: Boolean
        get() = dayOff?.kind == OfficialDayOff.Kind.DAY_OFF || holidays.any { it.category == HolidayCategory.PUBLIC_HOLIDAY }
}
