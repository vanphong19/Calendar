package com.app.calendar.core.data.reminder

import android.content.Context
import com.app.calendar.core.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {
    override fun requestReschedule() = RescheduleRemindersWorker.enqueueNow(context)
}
