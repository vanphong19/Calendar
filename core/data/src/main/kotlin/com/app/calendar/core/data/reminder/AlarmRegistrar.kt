package com.app.calendar.core.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/** Thin wrapper around [AlarmManager] that prefers exact alarms and degrades gracefully. */
class AlarmRegistrar @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService()

    /** Exact alarms need SCHEDULE_EXACT_ALARM on Android 12+, which the user may revoke. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    fun schedule(requestCode: Int, triggerAt: Instant, payload: ReminderPayload) {
        val manager = alarmManager ?: return
        val pending = pendingIntent(requestCode, payload)
        val at = triggerAt.toEpochMilli()
        if (canScheduleExact()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            // Fallback without the exact-alarm permission: delivered within a 10 minute window.
            manager.setWindow(AlarmManager.RTC_WAKEUP, at, INEXACT_WINDOW_MS, pending)
        }
    }

    fun cancel(requestCode: Int) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager?.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(requestCode: Int, payload: ReminderPayload): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_REMIND)
                .also { payload.writeTo(it, requestCode) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val INEXACT_WINDOW_MS = 10 * 60 * 1000L
    }
}

/** Data carried inside the alarm intent so the receiver never has to hit the database. */
data class ReminderPayload(
    val eventId: Long,
    val title: String,
    val occurrenceStart: String,
    val isAllDay: Boolean,
    val minutesBefore: Int,
) {
    fun writeTo(intent: Intent, notificationId: Int) {
        intent.putExtra(EXTRA_EVENT_ID, eventId)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_OCCURRENCE_START, occurrenceStart)
            .putExtra(EXTRA_ALL_DAY, isAllDay)
            .putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
            .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }

    companion object {
        const val EXTRA_EVENT_ID = "com.app.calendar.extra.EVENT_ID"
        const val EXTRA_TITLE = "com.app.calendar.extra.TITLE"
        const val EXTRA_OCCURRENCE_START = "com.app.calendar.extra.OCCURRENCE_START"
        const val EXTRA_ALL_DAY = "com.app.calendar.extra.ALL_DAY"
        const val EXTRA_MINUTES_BEFORE = "com.app.calendar.extra.MINUTES_BEFORE"
        const val EXTRA_NOTIFICATION_ID = "com.app.calendar.extra.NOTIFICATION_ID"

        fun readFrom(intent: Intent): ReminderPayload? {
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1).takeIf { it > 0 } ?: return null
            return ReminderPayload(
                eventId = eventId,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                occurrenceStart = intent.getStringExtra(EXTRA_OCCURRENCE_START) ?: return null,
                isAllDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false),
                minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 0),
            )
        }
    }
}
