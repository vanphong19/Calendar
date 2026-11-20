package com.app.calendar.feature.events.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.domain.usecase.DeleteScope
import com.app.calendar.feature.events.R
import com.app.calendar.feature.events.recurrenceLabel
import java.time.Duration
import com.app.calendar.core.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(DsR.string.action_back)) }
                },
                actions = {
                    val loaded = state as? EventDetailState.Loaded
                    if (loaded != null) {
                        IconButton(onClick = { onEdit(loaded.event.id) }) { Icon(Icons.Filled.Edit, stringResource(DsR.string.action_edit)) }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(DsR.string.action_delete)) }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when (val s = state) {
            EventDetailState.Loading -> Box(Modifier.fillMaxSize().padding(padding))
            EventDetailState.NotFound -> EmptyState(
                title = stringResource(R.string.events_detail_not_found),
                message = null,
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(DsR.string.action_back),
                onAction = onBack,
            )
            is EventDetailState.Loaded -> {
                val event = s.event
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.padding(top = 6.dp).size(width = 6.dp, height = 28.dp).clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.lich.event(event.color)),
                        )
                        Text(
                            event.title.ifBlank { stringResource(R.string.events_untitled) },
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val start = s.occurrenceDate.atTime(event.start.toLocalTime())
                        val end = start.plus(Duration.between(event.start, event.end))
                        Text(
                            LichFormat.weekdayFull(start.dayOfWeek) + ", " + LichFormat.solarDate(start.toLocalDate()) +
                                if (end.toLocalDate() != start.toLocalDate()) " – " + LichFormat.solarDate(end.toLocalDate()) else "",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        )
                        Text(
                            if (event.isAllDay) stringResource(DsR.string.all_day)
                            else stringResource(DsR.string.time_range, LichFormat.time(start.toLocalTime()), LichFormat.time(end.toLocalTime())),
                            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        )
                        Text(
                            stringResource(R.string.events_lunar_hint, "${s.lunar.day} " + LichFormat.lunarMonth(s.lunar).replaceFirstChar { it.lowercase() }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (event.recurrence != null) {
                        InfoLine(Icons.Filled.Refresh, recurrenceLabel(event.recurrence?.frequency, event.startDate, s.startLunar))
                    }
                    if (event.reminders.isEmpty()) {
                        InfoLine(Icons.Filled.Notifications, stringResource(R.string.events_no_reminders))
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp).size(20.dp),
                            )
                            Box(Modifier.width(12.dp).height(1.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                event.reminders.forEach { reminder ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                    ) {
                                        Text(
                                            LichFormat.reminder(reminder.minutesBefore, event.isAllDay),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (event.location.isNotBlank()) InfoLine(Icons.Filled.LocationOn, event.location)
                    if (event.note.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.events_field_note),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(event.note, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    val loaded = state as? EventDetailState.Loaded
    if (confirmDelete && loaded != null) {
        val neutralButtonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        if (loaded.event.recurrence == null) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.events_delete_title)) },
                text = { Text(stringResource(R.string.events_delete_message)) },
                confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(DeleteScope.WHOLE_SERIES) }) { Text(stringResource(DsR.string.action_delete)) } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }, colors = neutralButtonColors) { Text(stringResource(DsR.string.action_cancel)) } },
            )
        } else {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.events_delete_recurring_title)) },
                text = {
                    Column {
                        TextButton(onClick = { confirmDelete = false; viewModel.delete(DeleteScope.THIS_OCCURRENCE) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.events_delete_this), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                        TextButton(onClick = { confirmDelete = false; viewModel.delete(DeleteScope.WHOLE_SERIES) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.events_delete_all), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { confirmDelete = false }, colors = neutralButtonColors) { Text(stringResource(DsR.string.action_cancel)) } },
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Box(Modifier.width(12.dp).height(1.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
