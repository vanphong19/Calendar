package com.app.calendar.core.data.repository

import com.app.calendar.core.data.db.EventDao
import com.app.calendar.core.data.db.toEntity
import com.app.calendar.core.data.db.toModel
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Room is the single source of truth for events; works fully offline. */
internal class OfflineEventRepository @Inject constructor(
    private val dao: EventDao,
    private val clock: Clock,
) : EventRepository {

    override fun observeEventsTouching(from: LocalDate, to: LocalDate): Flow<List<Event>> =
        dao.observeTouching(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toModel() } }

    override fun observeEvent(id: Long): Flow<Event?> = dao.observe(id).map { it?.toModel() }

    override suspend fun getEvent(id: Long): Event? = dao.get(id)?.toModel()

    override suspend fun eventsWithReminders(): List<Event> = dao.withReminders().map { it.toModel() }

    override suspend fun upsert(event: Event): Long = dao.save(
        event = event.toEntity(clock.millis()),
        reminders = event.reminders.map { it.minutesBefore }.distinct(),
        exclusions = event.excludedDates.map { it.toEpochDay() },
    )

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun excludeOccurrence(id: Long, occurrenceDate: LocalDate) {
        val event = getEvent(id) ?: return
        upsert(event.copy(excludedDates = event.excludedDates + occurrenceDate))
    }
}
