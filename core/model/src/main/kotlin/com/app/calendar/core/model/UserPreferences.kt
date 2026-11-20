package com.app.calendar.core.model

import java.time.DayOfWeek

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val showLunar: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultReminderMinutes: Int? = 15,
    /** Minutes before 00:00 of an all-day event; 420 = 17:00 the evening before. */
    val defaultAllDayReminderMinutes: Int? = 7 * 60,
    val showHolidays: Boolean = true,
    val hiddenHolidayCategories: Set<HolidayCategory> = emptySet(),
    val dailyImagesEnabled: Boolean = true,
    val dailyImagesWifiOnly: Boolean = false,
) {
    fun isHolidayVisible(category: HolidayCategory): Boolean = showHolidays && category !in hiddenHolidayCategories
}
