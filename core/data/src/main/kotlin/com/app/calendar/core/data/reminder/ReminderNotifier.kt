package com.app.calendar.core.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.calendar.core.data.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Builds and posts reminder notifications in Vietnamese. */
object ReminderNotifier {
    const val CHANNEL_ID = "event_reminders"

    /** Extras read by the launcher activity to open the event. */
    const val EXTRA_OPEN_EVENT_ID = "com.app.calendar.extra.OPEN_EVENT_ID"
    const val EXTRA_OPEN_DATE = "com.app.calendar.extra.OPEN_DATE"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, notificationId: Int, payload: ReminderPayload) {
        if (!canPost(context)) return
        ensureChannel(context)
        val start = LocalDateTime.parse(payload.occurrenceStart)
        val title = payload.title.ifBlank { context.getString(R.string.reminder_untitled) }
        val text = describe(context, start, payload.isAllDay, payload.minutesBefore)

        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_EVENT_ID, payload.eventId)
            putExtra(EXTRA_OPEN_DATE, start.toLocalDate().toString())
        }
        val contentIntent = open?.let {
            PendingIntent.getActivity(context, notificationId, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_SNOOZE)
                .also { payload.writeTo(it, notificationId) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.reminder_action_snooze), snoozeIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing else to do.
        }
    }

    private fun describe(context: Context, start: LocalDateTime, isAllDay: Boolean, minutesBefore: Int): String {
        val today = LocalDate.now()
        val day = when (start.toLocalDate()) {
            today -> context.getString(R.string.reminder_today)
            today.plusDays(1) -> context.getString(R.string.reminder_tomorrow)
            else -> start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
        return when {
            isAllDay -> context.getString(R.string.reminder_all_day, day)
            minutesBefore == 0 -> context.getString(R.string.reminder_starting_now, start.format(TIME))
            else -> context.getString(R.string.reminder_starts_at, start.format(TIME), day)
        }
    }

    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
}
