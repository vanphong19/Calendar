package com.app.calendar.feature.events.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.component.EventRow
import com.app.calendar.core.designsystem.component.dayInkColor
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.feature.events.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.app.calendar.core.designsystem.R as DsR

@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    onAddEvent: (LocalDate) -> Unit,
    onOpenEvent: (EventOccurrence) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expandedFab = LocalConfiguration.current.screenHeightDp >= 480
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddEvent(state.today) },
                expanded = expandedFab,
                icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(DsR.string.action_add_event)) },
                text = { Text(stringResource(DsR.string.action_add_event)) },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "title") {
                Text(
                    stringResource(R.string.events_tab_title),
                    style = if (expandedFab) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = if (expandedFab) 16.dp else 8.dp,
                            bottom = if (expandedFab) 8.dp else 4.dp,
                        )
                        .semantics { heading() },
                )
            }
            item(key = "toggle") {
                val options = listOf(false to R.string.events_upcoming, true to R.string.events_past)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    options.forEachIndexed { index, (past, label) ->
                        SegmentedButton(
                            selected = state.showPast == past,
                            onClick = { viewModel.setShowPast(past) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(stringResource(label), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            val days = state.days
            when {
                days == null -> Unit
                days.isEmpty() && !state.showPast -> item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.events_list_empty_title),
                        message = stringResource(R.string.events_list_empty_message),
                        modifier = Modifier.padding(top = if (expandedFab) 48.dp else 16.dp),
                    )
                }
                days.isEmpty() -> item(key = "empty-past") {
                    EmptyState(
                        title = stringResource(R.string.events_past_empty),
                        message = null,
                        modifier = Modifier.padding(top = if (expandedFab) 48.dp else 16.dp),
                    )
                }
                else -> days.forEach { day ->
                    item(key = "d-${day.date}") { DayHeader(day, state.today) }
                    items(day.occurrences, key = { "e-${day.date}-${it.event.id}-${it.start}" }) { occurrence ->
                        EventRow(occurrence, onClick = { onOpenEvent(occurrence) }, modifier = Modifier.padding(start = 64.dp, end = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: EventDay, today: LocalDate) {
    val days = ChronoUnit.DAYS.between(today, day.date)
    val relative = when {
        days == 0L -> stringResource(R.string.events_relative_today)
        days == 1L -> stringResource(R.string.events_relative_tomorrow)
        days in 2..60 -> stringResource(R.string.events_relative_in_days, days.toInt())
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 2.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            style = LichTextStyles.GridDay.copy(fontSize = LichTextStyles.GridDay.fontSize * 1.5f),
            color = dayInkColor(day.date, isDayOff = false),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                LichFormat.weekdayFull(day.date.dayOfWeek) + ", " + LichFormat.solarDate(day.date),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.events_lunar_short, day.lunar.day, day.lunar.month),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        relative?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.lich.accent) }
    }
}
