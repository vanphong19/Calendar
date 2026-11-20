package com.app.calendar.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Repetition rule (a practical subset of RFC 5545 plus lunar rules used in Vietnam).
 *
 * The series ends at [until] (inclusive) or after [count] occurrences, whichever is set.
 */
data class Recurrence(
    val frequency: Frequency,
    val interval: Int = 1,
    /** Only for [Frequency.WEEKLY]; empty means "the weekday of the first occurrence". */
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val until: LocalDate? = null,
    val count: Int? = null,
) {
    init {
        require(interval >= 1) { "interval must be >= 1" }
        require(count == null || count >= 1) { "count must be >= 1" }
    }
}

enum class Frequency {
    DAILY,
    WEEKLY,
    /** Same day number every month; months without that day are skipped (RFC 5545). */
    MONTHLY,
    /** Same month and day every year; 29/2 only occurs in leap years. */
    YEARLY,
    /** Same lunar day every lunar month, e.g. mùng 1 and rằm offerings. */
    LUNAR_MONTHLY,
    /** Same lunar day and month every lunar year, e.g. giỗ, lunar birthdays. */
    LUNAR_YEARLY,
}
