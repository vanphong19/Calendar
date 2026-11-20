package com.app.calendar.core.domain.repository

import com.app.calendar.core.model.DailyContent
import com.app.calendar.core.model.Event
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialDayOff
import com.app.calendar.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface EventRepository {
    /** Events that may have an occurrence touching [from]..[to] (recurring series are always included if started). */
    fun observeEventsTouching(from: LocalDate, to: LocalDate): Flow<List<Event>>

    fun observeEvent(id: Long): Flow<Event?>

    suspend fun getEvent(id: Long): Event?

    /** All events that have at least one reminder; used by the reminder scheduler. */
    suspend fun eventsWithReminders(): List<Event>

    /** Inserts or updates; returns the event id. */
    suspend fun upsert(event: Event): Long

    suspend fun delete(id: Long)

    suspend fun excludeOccurrence(id: Long, occurrenceDate: LocalDate)
}

interface HolidayRepository {
    suspend fun rules(): List<HolidayRule>

    /** Officially announced days off / make-up workdays for [year]; empty when none was published. */
    suspend fun officialDaysOff(year: Int): List<OfficialDayOff>
}

interface DailyContentRepository {
    /**
     * Content for [date]. Holiday-specific content (keyed by holiday rule id) takes priority over
     * the date-keyed entry so lunar holidays get the right image whatever their Gregorian date.
     */
    suspend fun contentFor(date: LocalDate, holidayRuleIds: List<String>): DailyContent

    /** Cover content for a month (lịch treo tường). */
    suspend fun monthCover(month: YearMonth): DailyContent
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}

/** Keeps OS alarms in sync with stored reminders. Implemented in the data layer. */
interface ReminderScheduler {
    /** Asks for a (debounced, background) recomputation of all pending reminder alarms. */
    fun requestReschedule()
}

/** Connectivity information used to respect the "download images on Wi-Fi only" preference. */
interface NetworkMonitor {
    /** True when the active network is unmetered (Wi-Fi/Ethernet). */
    val isUnmetered: Flow<Boolean>
}
