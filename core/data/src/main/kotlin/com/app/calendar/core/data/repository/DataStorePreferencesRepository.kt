package com.app.calendar.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.ThemeMode
import com.app.calendar.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject

internal class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { it.toUserPreferences() }
        .distinctUntilChanged()

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toUserPreferences())) }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            themeMode = this[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
            showLunar = this[Keys.SHOW_LUNAR] ?: defaults.showLunar,
            firstDayOfWeek = this[Keys.FIRST_DAY]?.let { DayOfWeek.of(it) } ?: defaults.firstDayOfWeek,
            defaultReminderMinutes = if (contains(Keys.DEFAULT_REMINDER)) this[Keys.DEFAULT_REMINDER]?.takeIf { it >= 0 } else defaults.defaultReminderMinutes,
            defaultAllDayReminderMinutes = if (contains(Keys.DEFAULT_ALL_DAY_REMINDER)) {
                this[Keys.DEFAULT_ALL_DAY_REMINDER]?.takeIf { it >= 0 }
            } else {
                defaults.defaultAllDayReminderMinutes
            },
            showHolidays = this[Keys.SHOW_HOLIDAYS] ?: defaults.showHolidays,
            hiddenHolidayCategories = this[Keys.HIDDEN_HOLIDAY_CATEGORIES]
                ?.mapNotNull { runCatching { HolidayCategory.valueOf(it) }.getOrNull() }?.toSet()
                ?: defaults.hiddenHolidayCategories,
            dailyImagesEnabled = this[Keys.DAILY_IMAGES] ?: defaults.dailyImagesEnabled,
            dailyImagesWifiOnly = this[Keys.DAILY_IMAGES_WIFI_ONLY] ?: defaults.dailyImagesWifiOnly,
            countdownHolidayId = this[Keys.COUNTDOWN_HOLIDAY]?.takeIf { it.isNotBlank() },
        )
    }

    private fun MutablePreferences.write(p: UserPreferences) {
        this[Keys.THEME] = p.themeMode.name
        this[Keys.DYNAMIC_COLOR] = p.useDynamicColor
        this[Keys.SHOW_LUNAR] = p.showLunar
        this[Keys.FIRST_DAY] = p.firstDayOfWeek.value
        this[Keys.DEFAULT_REMINDER] = p.defaultReminderMinutes ?: NONE
        this[Keys.DEFAULT_ALL_DAY_REMINDER] = p.defaultAllDayReminderMinutes ?: NONE
        this[Keys.SHOW_HOLIDAYS] = p.showHolidays
        this[Keys.HIDDEN_HOLIDAY_CATEGORIES] = p.hiddenHolidayCategories.map { it.name }.toSet()
        this[Keys.DAILY_IMAGES] = p.dailyImagesEnabled
        this[Keys.DAILY_IMAGES_WIFI_ONLY] = p.dailyImagesWifiOnly
        this[Keys.COUNTDOWN_HOLIDAY] = p.countdownHolidayId.orEmpty()
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SHOW_LUNAR = booleanPreferencesKey("show_lunar")
        val FIRST_DAY = intPreferencesKey("first_day_of_week")
        val DEFAULT_REMINDER = intPreferencesKey("default_reminder_minutes")
        val DEFAULT_ALL_DAY_REMINDER = intPreferencesKey("default_all_day_reminder_minutes")
        val SHOW_HOLIDAYS = booleanPreferencesKey("show_holidays")
        val HIDDEN_HOLIDAY_CATEGORIES = stringSetPreferencesKey("hidden_holiday_categories")
        val DAILY_IMAGES = booleanPreferencesKey("daily_images")
        val DAILY_IMAGES_WIFI_ONLY = booleanPreferencesKey("daily_images_wifi_only")
        val COUNTDOWN_HOLIDAY = stringPreferencesKey("countdown_holiday_id")
    }

    private companion object {
        const val NONE = -1
    }
}
