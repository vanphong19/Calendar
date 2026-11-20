package com.app.calendar.feature.holidays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.DailyImageView
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayDetailScreen(viewModel: HolidayDetailViewModel, allowNetworkImages: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(DsR.string.action_back)) } },
            )
        },
    ) { padding ->
        val holiday = state.holiday
        val rule = state.rule
        if (state.isLoading) {
            Box(Modifier.padding(padding))
            return@Scaffold
        }
        if (holiday == null || rule == null) {
            EmptyState(stringResource(R.string.holidays_not_found), null, Modifier.padding(padding))
            return@Scaffold
        }
        val accent = when (holiday.category) {
            HolidayCategory.PUBLIC_HOLIDAY -> MaterialTheme.lich.sunday
            HolidayCategory.TRADITIONAL -> MaterialTheme.lich.bronze
            HolidayCategory.OBSERVANCE -> MaterialTheme.lich.festival
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.content?.image?.let { image ->
                DailyImageView(
                    image = image,
                    allowNetwork = allowNetworkImages,
                    contentDescription = state.content?.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)),
                )
            }
            Text(LichFormat.holidayCategory(holiday.category).uppercase(), style = LichTextStyles.Band, color = accent)
            Text(holiday.name, style = MaterialTheme.typography.headlineMedium)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val dateText = LichFormat.weekdayFull(holiday.date.dayOfWeek) + ", " + LichFormat.solarDate(holiday.date) +
                    (state.lastDate?.takeIf { it != holiday.date }?.let { " – " + LichFormat.solarDate(it) } ?: "")
                Text(dateText, style = MaterialTheme.typography.titleMedium)
                state.lunar?.let { Text(stringResource(R.string.holidays_lunar_date, it.day, it.month), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    when {
                        rule.calendar == CalendarSystem.SOLAR -> stringResource(R.string.holidays_solar_rule, rule.day, rule.month)
                        rule.lastDayOfMonth -> stringResource(R.string.holidays_lunar_last_day, LichFormat.lunarMonthName(rule.month))
                        else -> stringResource(R.string.holidays_lunar_rule, rule.day, rule.month)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rule.description.isNotBlank()) Text(rule.description, style = MaterialTheme.typography.bodyLarge)
            state.content?.caption?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.daysOff.isNotEmpty()) {
                Text(stringResource(R.string.holidays_days_off), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.holidays_range, LichFormat.solarDate(state.daysOff.first().date), LichFormat.solarDate(state.daysOff.last().date)) +
                        " · " + state.daysOff.first().reason,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.holidays_source, state.daysOff.first().source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.upcoming.isNotEmpty()) {
                Text(stringResource(R.string.holidays_upcoming_years), style = MaterialTheme.typography.titleSmall)
                state.upcoming.forEach { (_, date) ->
                    Text(LichFormat.weekdayFull(date.dayOfWeek) + ", " + LichFormat.solarDate(date), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
