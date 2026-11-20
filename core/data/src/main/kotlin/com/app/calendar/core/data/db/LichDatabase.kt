package com.app.calendar.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, ReminderEntity::class, ExclusionEntity::class, ScheduledAlarmEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LichDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao

    companion object {
        const val NAME = "lich.db"
    }
}
