package com.app.calendar.feature.events.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.domain.usecase.EventValidationError
import com.app.calendar.core.domain.usecase.SaveEventUseCase
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.Event
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import com.app.calendar.core.model.Reminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class EditorError { END_BEFORE_START, INVALID_UNTIL, SAVE_FAILED }

data class EventEditorState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val location: String = "",
    val note: String = "",
    val showMoreFields: Boolean = false,
    val isAllDay: Boolean = false,
    val start: LocalDateTime = LocalDateTime.now(),
    val end: LocalDateTime = LocalDateTime.now().plusHours(1),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val color: EventColor = EventColor.LACQUER,
    val frequency: Frequency? = null,
    val until: LocalDate? = null,
    val reminders: List<Int> = emptyList(),
    val excludedDates: Set<LocalDate> = emptySet(),
    val startLunar: LunarDate? = null,
    val error: EditorError? = null,
    val isSaving: Boolean = false,
    val savedEventId: Long? = null,
    val isDirty: Boolean = false,
) {
    val startDate: LocalDate get() = start.toLocalDate()
    val endDate: LocalDate get() = end.toLocalDate()
}

sealed interface EditorAction {
    data class Title(val value: String) : EditorAction
    data class Location(val value: String) : EditorAction
    data class Note(val value: String) : EditorAction
    data object ShowMoreFields : EditorAction
    data class AllDay(val value: Boolean) : EditorAction
    data class StartDate(val value: LocalDate) : EditorAction
    data class StartTime(val value: LocalTime) : EditorAction
    data class EndDate(val value: LocalDate) : EditorAction
    data class EndTime(val value: LocalTime) : EditorAction
    data class Color(val value: EventColor) : EditorAction
    data class Repeat(val value: Frequency?) : EditorAction
    data class Until(val value: LocalDate?) : EditorAction
    data class ToggleReminder(val minutes: Int) : EditorAction
    data class AddReminder(val minutes: Int) : EditorAction
    data object Save : EditorAction
    data object DismissError : EditorAction
}

@HiltViewModel(assistedFactory = EventEditorViewModel.Factory::class)
class EventEditorViewModel @AssistedInject constructor(
    @Assisted("eventId") private val eventId: Long?,
    @Assisted("date") private val date: LocalDate?,
    private val repository: EventRepository,
    private val preferences: PreferencesRepository,
    private val saveEvent: SaveEventUseCase,
    private val lunarCalendar: LunarCalendar,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(EventEditorState())
    val state: StateFlow<EventEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val existing = eventId?.let { repository.getEvent(it) }
        if (existing != null) {
            _state.value = EventEditorState(
                isLoading = false,
                isNew = false,
                title = existing.title,
                location = existing.location,
                note = existing.note,
                showMoreFields = existing.location.isNotBlank() || existing.note.isNotBlank(),
                isAllDay = existing.isAllDay,
                start = existing.start,
                end = existing.end,
                zoneId = existing.zoneId,
                color = existing.color,
                frequency = existing.recurrence?.frequency,
                until = existing.recurrence?.until,
                reminders = existing.reminders.map { it.minutesBefore },
                excludedDates = existing.excludedDates,
                startLunar = lunarCalendar.toLunar(existing.startDate),
            )
            return
        }
        val prefs = preferences.preferences.first()
        val now = LocalDateTime.now(clock)
        val day = date ?: now.toLocalDate()
        // Today: next full hour; another day: 09:00.
        val start = if (day == now.toLocalDate()) now.truncatedTo(ChronoUnit.HOURS).plusHours(1) else day.atTime(9, 0)
        _state.value = EventEditorState(
            isLoading = false,
            isNew = true,
            start = start,
            end = start.plusHours(1),
            reminders = listOfNotNull(prefs.defaultReminderMinutes),
            startLunar = lunarCalendar.toLunar(start.toLocalDate()),
        )
    }

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.Title -> edit { copy(title = action.value) }
            is EditorAction.Location -> edit { copy(location = action.value) }
            is EditorAction.Note -> edit { copy(note = action.value) }
            EditorAction.ShowMoreFields -> _state.update { it.copy(showMoreFields = true) }
            is EditorAction.AllDay -> viewModelScope.launch {
                val prefs = preferences.preferences.first()
                edit {
                    val defaultReminder = if (action.value) prefs.defaultAllDayReminderMinutes else prefs.defaultReminderMinutes
                    copy(
                        isAllDay = action.value,
                        end = if (action.value) end else if (!end.isAfter(start)) start.plusHours(1) else end,
                        // Reminder offsets mean different things for all-day and timed events.
                        reminders = if (reminders.isEmpty()) emptyList() else listOfNotNull(defaultReminder),
                    )
                }
            }
            is EditorAction.StartDate -> edit {
                val duration = Duration.between(start, end)
                val newStart = action.value.atTime(start.toLocalTime())
                copy(start = newStart, end = newStart.plus(duration), startLunar = lunarCalendar.toLunar(action.value))
            }
            is EditorAction.StartTime -> edit {
                val duration = Duration.between(start, end)
                val newStart = start.toLocalDate().atTime(action.value)
                copy(start = newStart, end = newStart.plus(duration))
            }
            is EditorAction.EndDate -> edit { copy(end = action.value.atTime(end.toLocalTime())) }
            is EditorAction.EndTime -> edit {
                var newEnd = end.toLocalDate().atTime(action.value)
                // Picking an earlier time on the same day most likely means "past midnight".
                if (!isAllDay && !newEnd.isAfter(start) && end.toLocalDate() == start.toLocalDate()) newEnd = newEnd.plusDays(1)
                copy(end = newEnd)
            }
            is EditorAction.Color -> edit { copy(color = action.value) }
            is EditorAction.Repeat -> edit { copy(frequency = action.value, until = if (action.value == null) null else until) }
            is EditorAction.Until -> edit { copy(until = action.value) }
            is EditorAction.ToggleReminder -> edit {
                copy(reminders = if (action.minutes in reminders) reminders - action.minutes else (reminders + action.minutes).sorted())
            }
            is EditorAction.AddReminder -> edit { copy(reminders = (reminders + action.minutes).distinct().sorted()) }
            EditorAction.Save -> save()
            EditorAction.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun edit(transform: EventEditorState.() -> EventEditorState) =
        _state.update { it.transform().copy(isDirty = true, error = null) }

    private fun save() {
        val s = _state.value
        if (s.isSaving) return
        val event = s.toEvent()
        when (saveEvent.validate(event)) {
            EventValidationError.EndBeforeStart -> return _state.update { it.copy(error = EditorError.END_BEFORE_START) }
            EventValidationError.InvalidRecurrenceEnd -> return _state.update { it.copy(error = EditorError.INVALID_UNTIL) }
            null -> Unit
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveEvent(event)
                .onSuccess { id -> _state.update { it.copy(isSaving = false, savedEventId = id, isDirty = false) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = EditorError.SAVE_FAILED) } }
        }
    }

    private fun EventEditorState.toEvent(): Event {
        val normalizedStart = if (isAllDay) start.toLocalDate().atStartOfDay() else start
        val normalizedEnd = if (isAllDay) {
            end.toLocalDate().coerceAtLeastDate(start.toLocalDate()).atStartOfDay()
        } else {
            end
        }
        return Event(
            id = eventId ?: 0,
            title = title,
            note = note,
            location = location,
            isAllDay = isAllDay,
            start = normalizedStart,
            end = normalizedEnd,
            zoneId = zoneId,
            color = color,
            recurrence = frequency?.let { Recurrence(it, until = until) },
            reminders = reminders.map(::Reminder),
            excludedDates = excludedDates,
        )
    }

    private fun LocalDate.coerceAtLeastDate(min: LocalDate) = if (isBefore(min)) min else this

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("eventId") eventId: Long?, @Assisted("date") date: LocalDate?): EventEditorViewModel
    }
}
