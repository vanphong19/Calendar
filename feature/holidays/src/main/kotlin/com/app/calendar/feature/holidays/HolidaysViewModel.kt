package com.app.calendar.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.holiday.breaksForHoliday
import com.app.calendar.core.domain.holiday.buildOfficialBreaks
import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.domain.repository.HolidayRepository
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.domain.usecase.HolidayCountdown
import com.app.calendar.core.domain.usecase.ObserveHolidayCountdownUseCase
import com.app.calendar.core.domain.usecase.UpcomingHolidaysUseCase
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.DailyContent
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialHolidayBreak
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A holiday occurrence in the list; multi-day holidays are collapsed into one row. */
data class HolidayListItem(val holiday: Holiday, val lastDate: LocalDate, val lunar: LunarDate)

data class HolidaysUiState(
    val year: Int,
    val today: LocalDate = LocalDate.now(),
    val countdown: HolidayCountdown? = null,
    val countdownLunar: LunarDate? = null,
    val upcoming: List<Holiday> = emptyList(),
    val filter: Set<HolidayCategory> = HolidayCategory.entries.toSet(),
    val items: List<HolidayListItem>? = null,
    val officialBreaks: List<OfficialHolidayBreak> = emptyList(),
    val hasOfficialAnnouncement: Boolean = false,
) {
    val totalOfficialDaysOff: Int get() = officialBreaks.sumOf { it.dayCount }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HolidaysViewModel @Inject constructor(
    private val holidayCalendar: HolidayCalendar,
    private val lunarCalendar: LunarCalendar,
    private val preferences: PreferencesRepository,
    upcomingHolidays: UpcomingHolidaysUseCase,
    observeCountdown: ObserveHolidayCountdownUseCase,
) : ViewModel() {
    private val today = LocalDate.now()
    private val countdown = observeCountdown(today)
    private val upcoming = flow { emit(upcomingHolidays(today)) }

    private val year = MutableStateFlow(today.year)
    private val filter = MutableStateFlow(HolidayCategory.entries.toSet())

    private val list = combine(year, filter) { y, f -> y to f }
        .mapLatest { (y, f) ->
            val data = holidayCalendar.year(y)
            val items = data.holidays
                .filter { it.category in f }
                .groupBy { it.ruleId to it.date.minusDays(it.dayIndex - 1L) }
                .map { (_, days) -> HolidayListItem(days.first(), days.last().date, lunarCalendar.toLunar(days.first().date)) }
                .sortedBy { it.holiday.date }
            val officialBreaks = buildOfficialBreaks(data)
            HolidaysUiState(
                year = y,
                today = today,
                filter = f,
                items = items,
                officialBreaks = officialBreaks,
                hasOfficialAnnouncement = data.daysOff.isNotEmpty(),
            )
        }

    val state: StateFlow<HolidaysUiState> = combine(list, countdown, upcoming) { base, c, u ->
        base.copy(
            countdown = c,
            countdownLunar = c?.let { lunarCalendar.toLunar(it.holiday.date) },
            upcoming = u,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HolidaysUiState(today.year, today = today))

    fun chooseCountdown(ruleId: String) {
        viewModelScope.launch { preferences.update { it.copy(countdownHolidayId = ruleId) } }
    }

    fun changeYear(delta: Int) = year.update { (it + delta).coerceIn(1901, 2099) }

    fun resetToCurrentYear() {
        year.value = today.year
    }

    fun toggle(category: HolidayCategory) = filter.update { if (category in it && it.size > 1) it - category else it + category }
}

data class UpcomingOccurrence(val year: Int, val date: LocalDate, val lunar: LunarDate)

data class HolidayDetailState(
    val rule: HolidayRule? = null,
    val holiday: Holiday? = null,
    val lastDate: LocalDate? = null,
    val lunar: LunarDate? = null,
    val content: DailyContent? = null,
    val officialBreaks: List<OfficialHolidayBreak> = emptyList(),
    val upcoming: List<UpcomingOccurrence> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel(assistedFactory = HolidayDetailViewModel.Factory::class)
class HolidayDetailViewModel @AssistedInject constructor(
    @Assisted private val ruleId: String,
    @Assisted private val year: Int,
    private val holidayCalendar: HolidayCalendar,
    private val holidayRepository: HolidayRepository,
    private val dailyContent: DailyContentRepository,
    private val lunarCalendar: LunarCalendar,
) : ViewModel() {

    val state: StateFlow<HolidayDetailState> = flow {
        val rule = holidayRepository.rules().firstOrNull { it.id == ruleId }
        val yearData = holidayCalendar.year(year)
        val days = yearData.holidays.filter { it.ruleId == ruleId }
        val first = days.firstOrNull()
        val officialBreaks = if (first == null) emptyList() else breaksForHoliday(ruleId, yearData)
        val upcoming = (year + 1..year + 5).mapNotNull { y ->
            holidayCalendar.year(y).holidays.firstOrNull { it.ruleId == ruleId && it.dayIndex == 1 }?.let {
                UpcomingOccurrence(y, it.date, lunarCalendar.toLunar(it.date))
            }
        }
        emit(
            HolidayDetailState(
                rule = rule,
                holiday = first,
                lastDate = days.lastOrNull()?.date,
                lunar = first?.let { lunarCalendar.toLunar(it.date) },
                content = first?.let { dailyContent.contentFor(it.date, listOf(ruleId)) },
                officialBreaks = officialBreaks,
                upcoming = upcoming,
                isLoading = false,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HolidayDetailState())

    @AssistedFactory
    interface Factory {
        fun create(ruleId: String, year: Int): HolidayDetailViewModel
    }
}
