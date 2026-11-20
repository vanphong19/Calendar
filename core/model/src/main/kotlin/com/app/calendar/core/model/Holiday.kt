package com.app.calendar.core.model

import java.time.LocalDate

enum class HolidayCategory {
    /** Ngày nghỉ lễ chính thức theo Bộ luật Lao động. */
    PUBLIC_HOLIDAY,
    /** Ngày lễ truyền thống theo âm lịch (Rằm tháng Giêng, Trung Thu, Vu Lan…). */
    TRADITIONAL,
    /** Ngày kỷ niệm (8/3, 20/10, 20/11…). */
    OBSERVANCE,
}

enum class CalendarSystem { SOLAR, LUNAR }

/**
 * How a holiday recurs. Lunar rules are converted to Gregorian dates per year via the lunar
 * calendar; nothing lunar is ever stored as a fixed Gregorian date.
 */
data class HolidayRule(
    val id: String,
    val name: String,
    val category: HolidayCategory,
    val calendar: CalendarSystem,
    val month: Int,
    /** Day of month; ignored when [lastDayOfMonth] is true. */
    val day: Int,
    val lastDayOfMonth: Boolean = false,
    val durationDays: Int = 1,
    val description: String = "",
    val sinceYear: Int? = null,
)

/** A holiday resolved to a concrete Gregorian date. */
data class Holiday(
    val ruleId: String,
    val name: String,
    val category: HolidayCategory,
    val date: LocalDate,
    val description: String,
    /** Calendar system the rule is defined in (for "10/3 âm lịch" style labels). */
    val calendar: CalendarSystem,
    /** 1-based day within a multi-day holiday (e.g. mùng 2 Tết), 1 for single-day holidays. */
    val dayIndex: Int = 1,
)

/** An official day-off / make-up workday announced for a specific year. */
data class OfficialDayOff(
    val date: LocalDate,
    val kind: Kind,
    val reason: String,
    /** Legal document or announcement the entry comes from. */
    val source: String,
) {
    enum class Kind { DAY_OFF, MAKE_UP_WORKDAY }
}

/** Use a holiday's dedicated image only on its first day; later days keep their own daily image. */
fun List<Holiday>.dailyContentRuleIds(): List<String> = filter { it.dayIndex == 1 }.map { it.ruleId }
