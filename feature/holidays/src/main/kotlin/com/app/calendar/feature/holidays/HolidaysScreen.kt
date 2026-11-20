package com.app.calendar.feature.holidays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.component.dayInkColor
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.OfficialDayOff
import com.app.calendar.core.designsystem.R as DsR

@Composable
fun HolidaysScreen(viewModel: HolidaysViewModel, onOpenHoliday: (ruleId: String, year: Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.changeYear(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.holidays_previous_year))
                }
                Text(
                    stringResource(R.string.holidays_title, state.year),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = { viewModel.changeYear(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.holidays_next_year))
                }
            }
        }
        item(key = "filters") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HolidayCategory.entries) { category ->
                    FilterChip(
                        selected = category in state.filter,
                        onClick = { viewModel.toggle(category) },
                        label = { Text(LichFormat.holidayCategory(category)) },
                    )
                }
            }
        }
        item(key = "days-off") { DaysOffSection(state.year, state.daysOff) }
        val items = state.items
        if (items != null && items.isEmpty()) {
            item(key = "empty") { EmptyState(title = stringResource(R.string.holidays_empty), message = null) }
        }
        items.orEmpty().groupBy { it.holiday.date.monthValue }.forEach { (month, monthItems) ->
            item(key = "m-$month") {
                Text(
                    LichFormat.monthName(month).uppercase(),
                    style = LichTextStyles.Band,
                    color = MaterialTheme.lich.accent,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp).semantics { heading() },
                )
            }
            items(monthItems, key = { "${it.holiday.ruleId}-${it.holiday.date}" }) { item ->
                HolidayListRow(item) { onOpenHoliday(item.holiday.ruleId, state.year) }
            }
        }
    }
}

@Composable
private fun HolidayListRow(item: HolidayListItem, onClick: () -> Unit) {
    val holiday = item.holiday
    val accent = when (holiday.category) {
        HolidayCategory.PUBLIC_HOLIDAY -> MaterialTheme.lich.sunday
        HolidayCategory.TRADITIONAL -> MaterialTheme.lich.bronze
        HolidayCategory.OBSERVANCE -> MaterialTheme.lich.festival
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                holiday.date.dayOfMonth.toString(),
                style = LichTextStyles.GridDay.copy(fontSize = LichTextStyles.GridDay.fontSize * 1.3f),
                color = dayInkColor(holiday.date, holiday.category == HolidayCategory.PUBLIC_HOLIDAY),
            )
            Text(LichFormat.weekdayShort(holiday.date.dayOfWeek), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(holiday.name, style = MaterialTheme.typography.titleSmall, color = accent)
            val sub = buildList {
                if (holiday.calendar == CalendarSystem.LUNAR) add(stringResource(R.string.holidays_lunar_date, item.lunar.day, item.lunar.month))
                if (item.lastDate != holiday.date) add(stringResource(R.string.holidays_range, LichFormat.solarDate(holiday.date), LichFormat.solarDate(item.lastDate)))
                add(LichFormat.holidayCategory(holiday.category))
            }.joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DaysOffSection(year: Int, daysOff: List<OfficialDayOff>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.holidays_days_off),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (daysOff.isEmpty()) {
            Text(stringResource(R.string.holidays_days_off_missing, year), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Group consecutive days with the same reason into ranges.
            val groups = mutableListOf<MutableList<OfficialDayOff>>()
            daysOff.forEach { day ->
                val last = groups.lastOrNull()?.lastOrNull()
                if (last != null && last.reason == day.reason && last.date.plusDays(1) == day.date) groups.last().add(day) else groups.add(mutableListOf(day))
            }
            groups.forEach { group ->
                val first = group.first()
                val range = if (group.size == 1) LichFormat.solarDate(first.date) else stringResource(R.string.holidays_range, LichFormat.solarDate(first.date), LichFormat.solarDate(group.last().date))
                Text(
                    range + " · " + stringResource(if (first.kind == OfficialDayOff.Kind.DAY_OFF) DsR.string.day_off_official else DsR.string.make_up_workday),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (first.kind == OfficialDayOff.Kind.DAY_OFF) MaterialTheme.lich.sunday else MaterialTheme.colorScheme.onSurface,
                )
                Text(first.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.holidays_source, daysOff.map { it.source }.distinct().joinToString("; ")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
