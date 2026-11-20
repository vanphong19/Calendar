package com.app.calendar.feature.calendar

import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.domain.repository.NetworkMonitor
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.domain.usecase.ObserveDayDetailsUseCase
import com.app.calendar.core.model.DailyContent
import com.app.calendar.core.model.DayDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** What one tear-off page shows. [content] is null until loaded. */
data class DayPageState(
    val details: DayDetails,
    val content: DailyContent?,
)

/** Display preferences relevant to calendar pages. */
data class PageSettings(
    val showLunar: Boolean = true,
    val showImages: Boolean = true,
    val allowNetworkImages: Boolean = true,
    val firstDayOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
)

/** Builds and caches per-day page flows so swiping back and forth never reloads. */
class DayPageLoader @Inject constructor(
    private val observeDayDetails: ObserveDayDetailsUseCase,
    private val dailyContent: DailyContentRepository,
    private val preferences: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) {
    private val cache = object : LinkedHashMap<LocalDate, StateFlow<DayPageState>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, StateFlow<DayPageState>>?) = size > MAX_PAGES
    }

    fun initial(date: LocalDate) = DayPageState(observeDayDetails.lunarOnly(date), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun page(date: LocalDate, scope: CoroutineScope): StateFlow<DayPageState> = synchronized(cache) {
        cache.getOrPut(date) {
            observeDayDetails(date)
                .mapLatest { details -> DayPageState(details, dailyContent.contentFor(date, details.holidays.map { it.ruleId })) }
                .stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial(date))
        }
    }

    fun settings(scope: CoroutineScope): StateFlow<PageSettings> =
        combine(preferences.preferences, networkMonitor.isUnmetered) { prefs, unmetered ->
            PageSettings(
                showLunar = prefs.showLunar,
                showImages = prefs.dailyImagesEnabled,
                allowNetworkImages = prefs.dailyImagesEnabled && (!prefs.dailyImagesWifiOnly || unmetered),
                firstDayOfWeek = prefs.firstDayOfWeek,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), PageSettings())

    /** The current local date, updated shortly after midnight. */
    fun today(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now(clock)
            emit(now.toLocalDate())
            val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
            delay(untilMidnight.coerceIn(1_000, 60_000))
        }
    }.distinctUntilChanged()

    private companion object {
        const val MAX_PAGES = 9
    }
}

/** Day index of the virtual pager: 1901-01-01 .. 2099-12-31. */
object DayPaging {
    val FIRST: LocalDate = LocalDate.of(1901, 1, 1)
    val LAST: LocalDate = LocalDate.of(2099, 12, 31)
    val PAGE_COUNT: Int = (LAST.toEpochDay() - FIRST.toEpochDay() + 1).toInt()

    fun pageOf(date: LocalDate): Int = (date.coerceIn(FIRST, LAST).toEpochDay() - FIRST.toEpochDay()).toInt()
    fun dateOf(page: Int): LocalDate = FIRST.plusDays(page.toLong())
}

private fun LocalDate.coerceIn(min: LocalDate, max: LocalDate) = if (isBefore(min)) min else if (isAfter(max)) max else this

internal fun <T> Flow<T>.asState(scope: CoroutineScope, initial: T) = stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

