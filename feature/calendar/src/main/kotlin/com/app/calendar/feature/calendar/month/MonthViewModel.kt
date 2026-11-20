package com.app.calendar.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.calendar.CalendarGrid
import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.domain.usecase.AgendaDay
import com.app.calendar.core.domain.usecase.ObserveAgendaUseCase
import com.app.calendar.core.domain.usecase.ObserveDayCellsUseCase
import com.app.calendar.core.model.DailyContent
import com.app.calendar.core.model.DayCell
import com.app.calendar.feature.calendar.DayPageLoader
import com.app.calendar.feature.calendar.DayPageState
import com.app.calendar.feature.calendar.PageSettings
import com.app.calendar.feature.calendar.asState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarMode { MONTH, WEEK, AGENDA }

sealed interface MonthAction {
    data class SetMode(val mode: CalendarMode) : MonthAction
    data class ShowMonth(val month: YearMonth) : MonthAction
    data class SelectDate(val date: LocalDate) : MonthAction
    data object GoToToday : MonthAction
    data object LoadMoreAgenda : MonthAction
}

data class MonthUiState(
    val mode: CalendarMode = CalendarMode.MONTH,
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val agendaMonths: Long = 3,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthViewModel @Inject constructor(
    private val observeDayCells: ObserveDayCellsUseCase,
    private val observeAgenda: ObserveAgendaUseCase,
    private val dailyContent: DailyContentRepository,
    private val loader: DayPageLoader,
) : ViewModel() {

    private val _state = MutableStateFlow(MonthUiState())
    val state: StateFlow<MonthUiState> = _state.asStateFlow()

    val today: StateFlow<LocalDate> = loader.today().asState(viewModelScope, LocalDate.now())
    val settings: StateFlow<PageSettings> = loader.settings(viewModelScope)

    private val monthCache = lruCache<YearMonth, StateFlow<List<DayCell>>>(5)
    private val weekCache = lruCache<LocalDate, StateFlow<List<DayCell>>>(5)
    private val coverCache = lruCache<YearMonth, StateFlow<DailyContent?>>(5)

    val selectedPage: StateFlow<DayPageState> = _state.map { it.selectedDate }.distinctUntilChanged()
        .flatMapLatest { loader.page(it, viewModelScope) }
        .asState(viewModelScope, loader.initial(LocalDate.now()))

    val agenda: StateFlow<List<AgendaDay>?> = _state
        .map { it.visibleMonth.atDay(1) to it.visibleMonth.plusMonths(it.agendaMonths).atDay(1).minusDays(1) }
        .distinctUntilChanged()
        .flatMapLatest { (from, to) -> observeAgenda(from, to) }
        .asState(viewModelScope, null)

    private val firstDayOfWeek = settings.map { it.firstDayOfWeek }.distinctUntilChanged()

    fun monthCells(month: YearMonth): StateFlow<List<DayCell>> = synchronized(monthCache) {
        monthCache.getOrPut(month) {
            firstDayOfWeek.flatMapLatest { first -> observeDayCells(CalendarGrid.monthDates(month, first), month) }
                .asState(viewModelScope, emptyList())
        }
    }

    fun weekCells(anyDay: LocalDate): StateFlow<List<DayCell>> {
        return synchronized(weekCache) {
            weekCache.getOrPut(anyDay) {
                firstDayOfWeek.flatMapLatest { first -> observeDayCells(CalendarGrid.weekDates(anyDay, first), null) }
                    .asState(viewModelScope, emptyList())
            }
        }
    }

    fun cover(month: YearMonth): StateFlow<DailyContent?> = synchronized(coverCache) {
        coverCache.getOrPut(month) { flow { emit(dailyContent.monthCover(month)) }.asState(viewModelScope, null) }
    }

    fun onAction(action: MonthAction) {
        when (action) {
            is MonthAction.SetMode -> _state.update { it.copy(mode = action.mode) }
            is MonthAction.ShowMonth -> _state.update { state ->
                if (state.visibleMonth == action.month) {
                    state
                } else {
                    // Keep the selection inside the visible month so the day list matches the grid.
                    val day = state.selectedDate.dayOfMonth.coerceAtMost(action.month.lengthOfMonth())
                    val selected = if (YearMonth.from(today.value) == action.month) today.value else action.month.atDay(day)
                    state.copy(visibleMonth = action.month, selectedDate = selected, agendaMonths = 3)
                }
            }
            is MonthAction.SelectDate -> _state.update { it.copy(selectedDate = action.date, visibleMonth = YearMonth.from(action.date)) }
            MonthAction.GoToToday -> _state.update { it.copy(selectedDate = today.value, visibleMonth = YearMonth.from(today.value)) }
            MonthAction.LoadMoreAgenda -> _state.update { it.copy(agendaMonths = it.agendaMonths + 3) }
        }
    }

    private fun <K, V> lruCache(max: Int) = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > max
    }
}

