package com.app.calendar.feature.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.ThemeMode
import java.time.DayOfWeek
import com.app.calendar.core.designsystem.R as DsR

private val TIMED_DEFAULTS = listOf(null, 0, 5, 10, 15, 30, 60, 24 * 60)
private val ALL_DAY_DEFAULTS = listOf(null, -7 * 60, 7 * 60, 15 * 60)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exactAllowed by remember { mutableStateOf(context.canScheduleExact()) }
    var reminderDialog by rememberSaveable { mutableStateOf<Boolean?>(null) } // true = all-day
    var cacheCleared by rememberSaveable { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exact = context.canScheduleExact()
        if (exact != exactAllowed) viewModel.onAlarmPermissionChanged()
        exactAllowed = exact
        refreshTick++
    }

    val p = prefs ?: return
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).semantics { heading() },
            )
        }
        item { Section(stringResource(R.string.settings_section_appearance)) }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                val modes = listOf(
                    ThemeMode.SYSTEM to R.string.settings_theme_system,
                    ThemeMode.LIGHT to R.string.settings_theme_light,
                    ThemeMode.DARK to R.string.settings_theme_dark,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = p.themeMode == mode,
                            onClick = { viewModel.update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                SwitchRow(stringResource(R.string.settings_dynamic_color), stringResource(R.string.settings_dynamic_color_summary), p.useDynamicColor) { v ->
                    viewModel.update { it.copy(useDynamicColor = v) }
                }
            }
        }

        item { Section(stringResource(R.string.settings_section_calendar)) }
        item {
            SwitchRow(stringResource(R.string.settings_show_lunar), stringResource(R.string.settings_show_lunar_summary), p.showLunar) { v ->
                viewModel.update { it.copy(showLunar = v) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.settings_first_day), style = MaterialTheme.typography.bodyLarge)
                val days = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    days.forEachIndexed { i, day ->
                        SegmentedButton(
                            selected = p.firstDayOfWeek == day,
                            onClick = { viewModel.update { it.copy(firstDayOfWeek = day) } },
                            shape = SegmentedButtonDefaults.itemShape(i, days.size),
                        ) { Text(LichFormat.weekdayFull(day)) }
                    }
                }
            }
        }

        item { Section(stringResource(R.string.settings_section_holidays)) }
        item {
            SwitchRow(stringResource(R.string.settings_show_holidays), null, p.showHolidays) { v -> viewModel.update { it.copy(showHolidays = v) } }
        }
        HolidayCategory.entries.forEach { category ->
            item {
                SwitchRow(
                    title = LichFormat.holidayCategory(category),
                    summary = null,
                    checked = p.showHolidays && category !in p.hiddenHolidayCategories,
                    enabled = p.showHolidays,
                    indent = true,
                ) { visible ->
                    viewModel.update {
                        it.copy(hiddenHolidayCategories = if (visible) it.hiddenHolidayCategories - category else it.hiddenHolidayCategories + category)
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.settings_holiday_data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        item { Section(stringResource(R.string.settings_section_reminders)) }
        item {
            ValueRow(stringResource(R.string.settings_default_reminder), LichFormat.reminder(p.defaultReminderMinutes, false)) { reminderDialog = false }
        }
        item {
            ValueRow(stringResource(R.string.settings_default_all_day_reminder), LichFormat.reminder(p.defaultAllDayReminderMinutes, true)) { reminderDialog = true }
        }
        item {
            ValueRow(
                stringResource(R.string.settings_notifications),
                stringResource(if (notificationsAllowed) R.string.settings_notifications_on else R.string.settings_notifications_off),
            ) { context.openNotificationSettings() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                ValueRow(
                    stringResource(R.string.settings_exact_alarm),
                    stringResource(if (exactAllowed) R.string.settings_exact_alarm_on else R.string.settings_exact_alarm_off),
                ) { context.openExactAlarmSettings() }
            }
        }

        item { Section(stringResource(R.string.settings_section_images)) }
        item {
            SwitchRow(stringResource(R.string.settings_daily_images), null, p.dailyImagesEnabled) { v -> viewModel.update { it.copy(dailyImagesEnabled = v) } }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_wifi_only),
                stringResource(R.string.settings_wifi_only_summary),
                p.dailyImagesWifiOnly,
                enabled = p.dailyImagesEnabled,
            ) { v -> viewModel.update { it.copy(dailyImagesWifiOnly = v) } }
        }
        item {
            ValueRow(
                stringResource(R.string.settings_clear_cache),
                if (cacheCleared) stringResource(R.string.settings_clear_cache_done) else null,
            ) {
                SingletonImageLoader.get(context).let { loader ->
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
                cacheCleared = true
            }
        }

        item { Section(stringResource(R.string.settings_section_about)) }
        item {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.settings_licenses), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Text(stringResource(R.string.settings_licenses_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    reminderDialog?.let { allDay ->
        val options = if (allDay) ALL_DAY_DEFAULTS else TIMED_DEFAULTS
        val current = if (allDay) p.defaultAllDayReminderMinutes else p.defaultReminderMinutes
        AlertDialog(
            onDismissRequest = { reminderDialog = null },
            title = { Text(stringResource(if (allDay) R.string.settings_default_all_day_reminder else R.string.settings_default_reminder)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = option == current, role = Role.RadioButton) {
                                    viewModel.update { if (allDay) it.copy(defaultAllDayReminderMinutes = option) else it.copy(defaultReminderMinutes = option) }
                                    reminderDialog = null
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == current, onClick = null)
                            Text(LichFormat.reminder(option, allDay), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { reminderDialog = null }) { Text(stringResource(DsR.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = 12.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.lich.accent,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    indent: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(start = if (indent) 36.dp else 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ValueRow(title: String, value: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        value?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun Context.canScheduleExact(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true

private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

