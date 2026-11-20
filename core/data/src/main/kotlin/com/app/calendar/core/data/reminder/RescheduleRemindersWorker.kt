package com.app.calendar.core.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.app.calendar.core.data.db.ScheduledAlarmDao
import com.app.calendar.core.data.db.ScheduledAlarmEntity
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.reminder.ReminderPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Diffs the reminders due in the next [HORIZON] against alarms already registered: cancels stale
 * ones, registers new ones. Idempotent, so it can safely run after every change, boot or zone change.
 */
@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val events: EventRepository,
    private val planner: ReminderPlanner,
    private val alarmDao: ScheduledAlarmDao,
    private val registrar: AlarmRegistrar,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = clock.instant()
        val triggers = planner.plan(events.eventsWithReminders(), now, HORIZON, clock.zone)
            .associateBy { it.key }
        val existing = alarmDao.all()

        // Cancel alarms whose reminder no longer exists, changed time, or already fired.
        val stale = existing.filter { alarm ->
            val trigger = triggers[alarm.key]
            trigger == null || trigger.triggerAt.toEpochMilli() != alarm.triggerAtMillis
        }
        stale.forEach { registrar.cancel(it.id) }
        if (stale.isNotEmpty()) alarmDao.delete(stale.map { it.id })

        val kept = existing.map { it.key }.toSet() - stale.map { it.key }.toSet()
        triggers.values.filter { it.key !in kept }.forEach { trigger ->
            val id = alarmDao.insert(ScheduledAlarmEntity(key = trigger.key, eventId = trigger.eventId, triggerAtMillis = trigger.triggerAt.toEpochMilli()))
            registrar.schedule(
                requestCode = id.toInt(),
                triggerAt = trigger.triggerAt,
                payload = ReminderPayload(trigger.eventId, trigger.eventTitle, trigger.occurrenceStart.toString(), trigger.isAllDay, trigger.minutesBefore),
            )
        }
        return Result.success()
    }

    companion object {
        val HORIZON: Duration = Duration.ofDays(8)
        private const val UNIQUE_NOW = "reschedule-reminders"
        private const val UNIQUE_PERIODIC = "reschedule-reminders-periodic"

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NOW,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
            )
        }

        /** Keeps the rolling window filled even if the app is not opened for days. */
        fun ensurePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RescheduleRemindersWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}
