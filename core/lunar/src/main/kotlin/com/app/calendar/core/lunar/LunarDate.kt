package com.app.calendar.core.lunar

/**
 * A date in the Vietnamese lunisolar calendar (âm lịch).
 *
 * @property day 1..30
 * @property month 1..12 (1 = tháng Giêng, 12 = tháng Chạp)
 * @property year lunar year number, aligned with the Gregorian year in which its Tết falls
 * @property isLeapMonth true when this is the intercalary (nhuận) month
 * @property monthLength 29 (tháng thiếu) or 30 (tháng đủ)
 */
data class LunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeapMonth: Boolean,
    val monthLength: Int,
) {
    val isFullMonth: Boolean get() = monthLength == 30
    val isFirstDayOfMonth: Boolean get() = day == 1
    val isFullMoonDay: Boolean get() = day == 15
}

/** One lunar month with the Julian day number of its first day. */
data class LunarMonth(
    val month: Int,
    val year: Int,
    val isLeap: Boolean,
    val startJdn: Int,
    val length: Int,
)
