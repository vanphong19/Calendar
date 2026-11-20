package com.app.calendar.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.calendar.core.data.db.LichDatabase
import com.app.calendar.core.data.repository.OfflineEventRepository
import com.app.calendar.core.model.Event
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.Frequency
import com.app.calendar.core.model.Recurrence
import com.app.calendar.core.model.Reminder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EventDaoTest {
    private lateinit var db: LichDatabase
    private lateinit var repository: OfflineEventRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LichDatabase::class.java).allowMainThreadQueries().build()
        repository = OfflineEventRepository(db.eventDao(), Clock.systemUTC())
    }

    @After
    fun tearDown() = db.close()

    private fun event(start: String, end: String, recurrence: Recurrence? = null) = Event(
        title = "Giỗ ông",
        note = "Mua hoa",
        location = "Hà Nội",
        isAllDay = false,
        start = LocalDateTime.parse(start),
        end = LocalDateTime.parse(end),
        zoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
        color = EventColor.LOTUS,
        recurrence = recurrence,
        reminders = listOf(Reminder(15), Reminder(60)),
    )

    @Test
    fun `round trips every field`() = runTest {
        val rule = Recurrence(Frequency.WEEKLY, interval = 2, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), until = LocalDate.of(2027, 1, 1))
        val id = repository.upsert(event("2026-11-20T09:30", "2026-11-20T11:00", rule))
        val loaded = repository.getEvent(id)!!
        assertThat(loaded).isEqualTo(event("2026-11-20T09:30", "2026-11-20T11:00", rule).copy(id = id))
    }

    @Test
    fun `update replaces reminders and exclusions`() = runTest {
        val id = repository.upsert(event("2026-11-20T09:00", "2026-11-20T10:00", Recurrence(Frequency.DAILY)))
        repository.excludeOccurrence(id, LocalDate.of(2026, 11, 22))
        repository.upsert(repository.getEvent(id)!!.copy(reminders = listOf(Reminder(5))))
        val loaded = repository.getEvent(id)!!
        assertThat(loaded.reminders).containsExactly(Reminder(5))
        assertThat(loaded.excludedDates).containsExactly(LocalDate.of(2026, 11, 22))
    }

    @Test
    fun `range query returns overlapping and still-running recurring events`() = runTest {
        repository.upsert(event("2026-11-20T09:00", "2026-11-20T10:00"))
        repository.upsert(event("2026-10-01T09:00", "2026-10-01T10:00"))
        repository.upsert(event("2026-01-01T09:00", "2026-01-01T10:00", Recurrence(Frequency.MONTHLY)))
        repository.upsert(event("2026-01-01T09:00", "2026-01-01T10:00", Recurrence(Frequency.DAILY, until = LocalDate.of(2026, 2, 1))))
        val result = repository.observeEventsTouching(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)).first()
        assertThat(result.map { it.startDate.toString() to (it.recurrence?.frequency) }).containsExactly(
            "2026-01-01" to Frequency.MONTHLY,
            "2026-11-20" to null,
        )
    }

    @Test
    fun `deleting cascades to reminders`() = runTest {
        val id = repository.upsert(event("2026-11-20T09:00", "2026-11-20T10:00"))
        repository.delete(id)
        assertThat(repository.getEvent(id)).isNull()
        assertThat(repository.eventsWithReminders()).isEmpty()
    }
}
