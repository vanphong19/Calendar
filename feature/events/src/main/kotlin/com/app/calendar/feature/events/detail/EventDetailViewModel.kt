package com.app.calendar.feature.events.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.usecase.DeleteEventUseCase
import com.app.calendar.core.domain.usecase.DeleteScope
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.Event
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface EventDetailState {
    data object Loading : EventDetailState
    data object NotFound : EventDetailState
    data class Loaded(
        val event: Event,
        val occurrenceDate: LocalDate,
        val lunar: LunarDate,
        val startLunar: LunarDate,
    ) : EventDetailState
}

@HiltViewModel(assistedFactory = EventDetailViewModel.Factory::class)
class EventDetailViewModel @AssistedInject constructor(
    @Assisted private val eventId: Long,
    @Assisted private val occurrenceDate: LocalDate?,
    repository: EventRepository,
    private val deleteEvent: DeleteEventUseCase,
    private val lunarCalendar: LunarCalendar,
) : ViewModel() {

    val state: StateFlow<EventDetailState> = repository.observeEvent(eventId).map { event ->
        if (event == null) {
            EventDetailState.NotFound
        } else {
            val date = occurrenceDate ?: event.startDate
            EventDetailState.Loaded(event, date, lunarCalendar.toLunar(date), lunarCalendar.toLunar(event.startDate))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun delete(scope: DeleteScope) {
        viewModelScope.launch {
            deleteEvent(eventId, scope, occurrenceDate)
            _deleted.value = true
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(eventId: Long, occurrenceDate: LocalDate?): EventDetailViewModel
    }
}
