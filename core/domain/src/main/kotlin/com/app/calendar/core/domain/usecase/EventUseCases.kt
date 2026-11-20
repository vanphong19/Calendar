package com.app.calendar.core.domain.usecase

import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.repository.ReminderScheduler
import com.app.calendar.core.model.Event
import java.time.LocalDate
import javax.inject.Inject

sealed interface EventValidationError {
    data object EndBeforeStart : EventValidationError
    data object InvalidRecurrenceEnd : EventValidationError
}

class SaveEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    fun validate(event: Event): EventValidationError? = when {
        event.end.isBefore(event.start) -> EventValidationError.EndBeforeStart
        event.recurrence?.until?.isBefore(event.startDate) == true -> EventValidationError.InvalidRecurrenceEnd
        else -> null
    }

    /** Saves [event] (normalising blank fields) and returns its id. */
    suspend operator fun invoke(event: Event): Result<Long> {
        validate(event)?.let { return Result.failure(IllegalArgumentException(it.toString())) }
        val normalised = event.copy(
            title = event.title.trim(),
            note = event.note.trim(),
            location = event.location.trim(),
            reminders = event.reminders.distinct().sortedBy { it.minutesBefore },
            excludedDates = if (event.recurrence == null) emptySet() else event.excludedDates,
        )
        val id = repository.upsert(normalised)
        reminderScheduler.requestReschedule()
        return Result.success(id)
    }
}

enum class DeleteScope { THIS_OCCURRENCE, WHOLE_SERIES }

class DeleteEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(eventId: Long, scope: DeleteScope, occurrenceDate: LocalDate? = null) {
        when (scope) {
            DeleteScope.WHOLE_SERIES -> repository.delete(eventId)
            DeleteScope.THIS_OCCURRENCE -> {
                val event = repository.getEvent(eventId) ?: return
                if (event.recurrence == null || occurrenceDate == null) {
                    repository.delete(eventId)
                } else {
                    repository.excludeOccurrence(eventId, occurrenceDate)
                }
            }
        }
        reminderScheduler.requestReschedule()
    }
}
