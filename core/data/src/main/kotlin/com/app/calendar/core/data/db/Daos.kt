package com.app.calendar.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Transaction
    @Query(
        """
        SELECT * FROM events
        WHERE start_epoch_day <= :toEpochDay
          AND (end_epoch_day >= :fromEpochDay
               OR (rec_frequency IS NOT NULL AND (rec_until_epoch_day IS NULL OR rec_until_epoch_day >= :fromEpochDay - (end_epoch_day - start_epoch_day))))
        ORDER BY start_epoch_day, start_minute
        """,
    )
    fun observeTouching(fromEpochDay: Long, toEpochDay: Long): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: Long): Flow<EventWithDetails?>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventWithDetails?

    @Transaction
    @Query("SELECT * FROM events WHERE id IN (SELECT DISTINCT event_id FROM event_reminders)")
    suspend fun withReminders(): List<EventWithDetails>

    @Upsert
    suspend fun upsertEvent(event: EventEntity): Long

    @Query("DELETE FROM event_reminders WHERE event_id = :eventId")
    suspend fun clearReminders(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM event_exclusions WHERE event_id = :eventId")
    suspend fun clearExclusions(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExclusions(exclusions: List<ExclusionEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun save(event: EventEntity, reminders: List<Int>, exclusions: List<Long>): Long {
        val returned = upsertEvent(event)
        val id = if (event.id == 0L) returned else event.id
        clearReminders(id)
        insertReminders(reminders.map { ReminderEntity(id, it) })
        clearExclusions(id)
        insertExclusions(exclusions.map { ExclusionEntity(id, it) })
        return id
    }
}

@Dao
interface ScheduledAlarmDao {
    @Query("SELECT * FROM scheduled_alarms")
    suspend fun all(): List<ScheduledAlarmEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(alarm: ScheduledAlarmEntity): Long

    @Query("DELETE FROM scheduled_alarms WHERE id IN (:ids)")
    suspend fun delete(ids: List<Int>)
}
