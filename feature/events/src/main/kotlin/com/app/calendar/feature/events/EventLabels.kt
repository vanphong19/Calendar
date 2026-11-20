package com.app.calendar.feature.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.Frequency
import java.time.LocalDate

/** Human description of a repetition rule anchored at [start]. */
@Composable
@ReadOnlyComposable
internal fun recurrenceLabel(frequency: Frequency?, start: LocalDate, lunar: LunarDate?): String = when (frequency) {
    null -> stringResource(R.string.events_repeat_none)
    Frequency.DAILY -> stringResource(R.string.events_repeat_daily)
    Frequency.WEEKLY -> stringResource(R.string.events_repeat_weekly, LichFormat.weekdayFull(start.dayOfWeek))
    Frequency.MONTHLY -> stringResource(R.string.events_repeat_monthly, start.dayOfMonth)
    Frequency.YEARLY -> stringResource(R.string.events_repeat_yearly, "%02d/%02d".format(start.dayOfMonth, start.monthValue))
    Frequency.LUNAR_MONTHLY -> stringResource(R.string.events_repeat_lunar_monthly, lunar?.day ?: 1)
    Frequency.LUNAR_YEARLY -> stringResource(
        R.string.events_repeat_lunar_yearly,
        lunar?.let { "${it.day}/${it.month}" } ?: "",
    )
}
