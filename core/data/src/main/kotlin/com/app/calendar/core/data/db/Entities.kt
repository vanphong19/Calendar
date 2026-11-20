package com.app.calendar.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "events",
    indices = [Index("start_epoch_day"), Index("end_epoch_day")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String,
    val location: String,
    @androidx.room.ColumnInfo(name = "is_all_day") val isAllDay: Boolean,
    @androidx.room.ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    @androidx.room.ColumnInfo(name = "start_minute") val startMinute: Int,
    @androidx.room.ColumnInfo(name = "end_epoch_day") val endEpochDay: Long,
    @androidx.room.ColumnInfo(name = "end_minute") val endMinute: Int,
    @androidx.room.ColumnInfo(name = "zone_id") val zoneId: String,
    val color: String,
    @androidx.room.ColumnInfo(name = "rec_frequency") val recurrenceFrequency: String?,
    @androidx.room.ColumnInfo(name = "rec_interval") val recurrenceInterval: Int,
    /** Bit i set = DayOfWeek.of(i + 1). */
    @androidx.room.ColumnInfo(name = "rec_days_mask") val recurrenceDaysMask: Int,
    @androidx.room.ColumnInfo(name = "rec_until_epoch_day") val recurrenceUntilEpochDay: Long?,
    @androidx.room.ColumnInfo(name = "rec_count") val recurrenceCount: Int?,
    @androidx.room.ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "event_reminders",
    primaryKeys = ["event_id", "minutes_before"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["event_id"], onDelete = ForeignKey.CASCADE)],
)
data class ReminderEntity(
    @androidx.room.ColumnInfo(name = "event_id") val eventId: Long,
    @androidx.room.ColumnInfo(name = "minutes_before") val minutesBefore: Int,
)

@Entity(
    tableName = "event_exclusions",
    primaryKeys = ["event_id", "epoch_day"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["event_id"], onDelete = ForeignKey.CASCADE)],
)
data class ExclusionEntity(
    @androidx.room.ColumnInfo(name = "event_id") val eventId: Long,
    @androidx.room.ColumnInfo(name = "epoch_day") val epochDay: Long,
)

/** Alarms currently registered with AlarmManager; the row id is the PendingIntent request code. */
@Entity(tableName = "scheduled_alarms", indices = [Index(value = ["alarm_key"], unique = true)])
data class ScheduledAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @androidx.room.ColumnInfo(name = "alarm_key") val key: String,
    @androidx.room.ColumnInfo(name = "event_id") val eventId: Long,
    @androidx.room.ColumnInfo(name = "trigger_at") val triggerAtMillis: Long,
)

data class EventWithDetails(
    @Embedded val event: EventEntity,
    @Relation(parentColumn = "id", entityColumn = "event_id") val reminders: List<ReminderEntity>,
    @Relation(parentColumn = "id", entityColumn = "event_id") val exclusions: List<ExclusionEntity>,
)
