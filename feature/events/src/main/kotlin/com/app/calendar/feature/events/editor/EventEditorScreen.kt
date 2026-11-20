package com.app.calendar.feature.events.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.Frequency
import com.app.calendar.feature.events.R
import com.app.calendar.feature.events.recurrenceLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import com.app.calendar.core.designsystem.R as DsR

private val TIMED_REMINDER_PRESETS = listOf(0, 5, 15, 30, 60, 24 * 60)
/** All-day offsets (minutes before 00:00): 07:00 same day, 17:00 and 09:00 the day before. */
private val ALL_DAY_REMINDER_PRESETS = listOf(-7 * 60, 7 * 60, 15 * 60)

private enum class PickerTarget { START_DATE, START_TIME, END_DATE, END_TIME, UNTIL }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(
    viewModel: EventEditorViewModel,
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var showRepeat by rememberSaveable { mutableStateOf(false) }
    var showCustomReminder by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.savedEventId) { state.savedEventId?.let(onSaved) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onAction(EditorAction.Save) // Save either way; reminders stay stored and work once allowed.
    }
    val save = {
        val needsPermission = state.reminders.isNotEmpty() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.onAction(EditorAction.Save)
    }
    val close = { if (state.isDirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.events_new_title else R.string.events_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, contentDescription = stringResource(DsR.string.action_close)) }
                },
                actions = {
                    Button(
                        onClick = save,
                        enabled = !state.isLoading && !state.isSaving,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(DsR.string.action_save)) }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            TitleField(state.title, autoFocus = state.isNew) { viewModel.onAction(EditorAction.Title(it)) }
            HorizontalDivider()

            SettingRow(label = stringResource(R.string.events_all_day)) {
                Switch(checked = state.isAllDay, onCheckedChange = { viewModel.onAction(EditorAction.AllDay(it)) })
            }
            DateTimeRow(
                label = stringResource(R.string.events_starts),
                date = state.startDate,
                time = state.start.toLocalTime().takeUnless { state.isAllDay },
                onDate = { picker = PickerTarget.START_DATE },
                onTime = { picker = PickerTarget.START_TIME },
            )
            DateTimeRow(
                label = stringResource(R.string.events_ends),
                date = state.endDate,
                time = state.end.toLocalTime().takeUnless { state.isAllDay },
                onDate = { picker = PickerTarget.END_DATE },
                onTime = { picker = PickerTarget.END_TIME },
            )
            state.startLunar?.let { lunar ->
                Text(
                    stringResource(R.string.events_lunar_hint, "${lunar.day} " + LichFormat.lunarMonth(lunar).lowercase()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            state.error?.let { ErrorText(it) }
            HorizontalDivider(Modifier.padding(top = 12.dp))

            ClickableRow(
                label = stringResource(R.string.events_repeat),
                value = recurrenceLabel(state.frequency, state.startDate, state.startLunar),
                onClick = { showRepeat = true },
            )
            if (state.frequency != null) {
                ClickableRow(
                    label = stringResource(R.string.events_repeat_until),
                    value = state.until?.let { stringResource(R.string.events_repeat_until_value, LichFormat.solarDate(it)) }
                        ?: stringResource(R.string.events_repeat_forever),
                    onClick = { picker = PickerTarget.UNTIL },
                )
            }
            HorizontalDivider()

            SectionLabel(stringResource(R.string.events_reminders))
            FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val presets = if (state.isAllDay) ALL_DAY_REMINDER_PRESETS else TIMED_REMINDER_PRESETS
                (presets + state.reminders).distinct().sorted().forEach { minutes ->
                    FilterChip(
                        selected = minutes in state.reminders,
                        onClick = { viewModel.onAction(EditorAction.ToggleReminder(minutes)) },
                        label = { Text(LichFormat.reminder(minutes, state.isAllDay)) },
                    )
                }
                if (!state.isAllDay) {
                    FilterChip(
                        selected = false,
                        onClick = { showCustomReminder = true },
                        label = { Text(stringResource(R.string.events_reminder_add_custom)) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp))

            SectionLabel(stringResource(R.string.events_color))
            ColorPicker(state.color) { viewModel.onAction(EditorAction.Color(it)) }
            HorizontalDivider(Modifier.padding(top = 12.dp))

            if (state.showMoreFields) {
                PlainField(state.location, stringResource(R.string.events_field_location), singleLine = true) {
                    viewModel.onAction(EditorAction.Location(it))
                }
                PlainField(state.note, stringResource(R.string.events_field_note), singleLine = false) {
                    viewModel.onAction(EditorAction.Note(it))
                }
            } else {
                TextButton(onClick = { viewModel.onAction(EditorAction.ShowMoreFields) }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.events_more_details))
                }
            }
        }
    }

    when (picker) {
        PickerTarget.START_DATE -> DateDialog(state.startDate, { picker = null }) { picker = null; it?.let { d -> viewModel.onAction(EditorAction.StartDate(d)) } }
        PickerTarget.END_DATE -> DateDialog(state.endDate, { picker = null }) { picker = null; it?.let { d -> viewModel.onAction(EditorAction.EndDate(d)) } }
        PickerTarget.UNTIL -> DateDialog(state.until ?: state.startDate.plusMonths(1), { picker = null }, allowClear = true) {
            picker = null
            viewModel.onAction(EditorAction.Until(it))
        }
        PickerTarget.START_TIME -> TimeDialog(state.start.toLocalTime(), { picker = null }) { picker = null; viewModel.onAction(EditorAction.StartTime(it)) }
        PickerTarget.END_TIME -> TimeDialog(state.end.toLocalTime(), { picker = null }) { picker = null; viewModel.onAction(EditorAction.EndTime(it)) }
        null -> Unit
    }
    if (showRepeat) {
        RepeatDialog(state, onDismiss = { showRepeat = false }) {
            showRepeat = false
            viewModel.onAction(EditorAction.Repeat(it))
        }
    }
    if (showCustomReminder) {
        CustomReminderDialog(onDismiss = { showCustomReminder = false }) {
            showCustomReminder = false
            viewModel.onAction(EditorAction.AddReminder(it))
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.events_discard_title)) },
            text = { Text(stringResource(R.string.events_discard_message)) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text(stringResource(R.string.events_discard_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.events_keep_editing)) } },
        )
    }
}

@Composable
private fun TitleField(value: String, autoFocus: Boolean, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.events_field_title_placeholder), style = MaterialTheme.typography.headlineSmall) },
        textStyle = MaterialTheme.typography.headlineSmall,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .focusRequester(focus),
    )
}

@Composable
private fun PlainField(value: String, label: String, singleLine: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun DateTimeRow(label: String, date: LocalDate, time: LocalTime?, onDate: () -> Unit, onTime: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp).weight(1f))
        TextButton(onClick = onDate) {
            Text(LichFormat.weekdayShort(date.dayOfWeek) + ", " + LichFormat.solarDate(date))
        }
        if (time != null) {
            TextButton(onClick = onTime) { Text(LichFormat.time(time)) }
        }
    }
}

@Composable
private fun ClickableRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun ErrorText(error: EditorError) {
    Text(
        stringResource(
            when (error) {
                EditorError.END_BEFORE_START -> R.string.events_error_end_before_start
                EditorError.INVALID_UNTIL -> R.string.events_error_until
                EditorError.SAVE_FAILED -> R.string.events_error_save
            },
        ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun ColorPicker(selected: EventColor, onSelect: (EventColor) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EventColor.entries.forEach { color ->
            val name = LichFormat.eventColorName(color)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.lich.event(color))
                    .then(if (color == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .selectable(selected = color == selected, role = Role.RadioButton, onClick = { onSelect(color) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                if (color == selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RepeatDialog(state: EventEditorState, onDismiss: () -> Unit, onSelect: (Frequency?) -> Unit) {
    val options = listOf(null) + Frequency.entries
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.events_repeat)) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(selected = state.frequency == option, role = Role.RadioButton, onClick = { onSelect(option) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.frequency == option, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(recurrenceLabel(option, state.startDate, state.startLunar))
                    }
                }
                Text(
                    stringResource(R.string.events_repeat_lunar_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    )
}

@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("10") }
    var unit by rememberSaveable { mutableStateOf(0) } // 0 minutes, 1 hours, 2 days
    val units = listOf(R.string.events_unit_minutes, R.string.events_unit_hours, R.string.events_unit_days)
    val multiplier = listOf(1, 60, 24 * 60)
    val value = amount.toIntOrNull()?.takeIf { it in 1..999 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.events_custom_reminder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    units.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = unit == index,
                            onClick = { unit = index },
                            shape = SegmentedButtonDefaults.itemShape(index, units.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onConfirm(it * multiplier[unit]) } }, enabled = value != null) {
                Text(stringResource(DsR.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, allowClear: Boolean = false, onPicked: (LocalDate?) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(DsR.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (allowClear) TextButton(onClick = { onPicked(null) }) { Text(stringResource(R.string.events_repeat_forever)) }
                TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) }
            }
        },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(DsR.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    )
}
