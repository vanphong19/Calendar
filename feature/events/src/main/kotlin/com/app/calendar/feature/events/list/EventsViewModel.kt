package com.app.calendar.feature.events.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.recurrence.RecurrenceExpander
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.EventOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class EventDay(val date: LocalDate, val lunar: LunarDate, val occurrences: List<EventOccurrence>)

data class EventsUiState(val showPast: Boolean = false, val today: LocalDate, val days: List<EventDay>? = null)

/** Personal events only (no holidays), grouped by day: the next 12 months or the past 6 months. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository,
    private val expander: RecurrenceExpander,
    private val lunarCalendar: LunarCalendar,
    private val clock: Clock,
) : ViewModel() {
    private val showPast = MutableStateFlow(false)

    val state: StateFlow<EventsUiState> = showPast.flatMapLatest { past ->
        val today = LocalDate.now(clock)
        val (from, to) = if (past) today.minusMonths(6) to today.minusDays(1) else today to today.plusYears(1)
        repository.observeEventsTouching(from, to).map { events ->
            val byDay = events.flatMap { expander.expand(it, from, to) }
                .groupBy { if (it.date.isBefore(from)) from else it.date }
                .toSortedMap(if (past) reverseOrder() else naturalOrder())
                .map { (date, list) -> EventDay(date, lunarCalendar.toLunar(date), list.sortedWith(compareBy({ !it.event.isAllDay }, { it.start }))) }
            EventsUiState(past, today, byDay)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState(today = LocalDate.now(clock)))

    fun setShowPast(value: Boolean) {
        showPast.value = value
    }
}
