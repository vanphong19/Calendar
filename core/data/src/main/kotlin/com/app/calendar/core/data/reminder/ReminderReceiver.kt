package com.app.calendar.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import java.time.Instant

/** Fires reminder notifications and handles the "snooze" action. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = ReminderPayload.readFrom(intent) ?: return
        val notificationId = intent.getIntExtra(ReminderPayload.EXTRA_NOTIFICATION_ID, payload.hashCode())
        when (intent.action) {
            ACTION_REMIND -> ReminderNotifier.show(context, notificationId, payload)
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                // Negative request codes never collide with scheduled_alarms row ids.
                AlarmRegistrar(context.applicationContext).schedule(
                    requestCode = -notificationId.coerceAtLeast(1),
                    triggerAt = Instant.now().plusSeconds(SNOOZE_SECONDS),
                    payload = payload,
                )
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.app.calendar.action.REMIND"
        const val ACTION_SNOOZE = "com.app.calendar.action.SNOOZE"
        private const val SNOOZE_SECONDS = 10 * 60L
    }
}

/** Re-plans alarms whenever the clock, time zone, device boot or app version changes. */
class SystemChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RescheduleRemindersWorker.enqueueNow(context)
    }
}
