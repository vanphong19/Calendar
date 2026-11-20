package com.app.calendar.feature.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exact = context.canScheduleExact()
        if (exact != exactAllowed) viewModel.onAlarmPermissionChanged()
        exactAllowed = exact
    }

    val p = prefs ?: return
    val compactHeight = LocalConfiguration.current.screenHeightDp < 480
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "title") {
                Text(
                    stringResource(R.string.settings_title),
                    style = if (compactHeight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = if (compactHeight) 10.dp else 16.dp)
                        .semantics { heading() },
                )
            }

            item(key = "section-appearance") {
                SectionHeader(stringResource(R.string.settings_section_appearance), topPadding = 4.dp)
                SettingsCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                                ) { Text(stringResource(label), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CardDivider()
                        SwitchRow(
                            title = stringResource(R.string.settings_dynamic_color),
                            summary = stringResource(R.string.settings_dynamic_color_summary),
                            checked = p.useDynamicColor,
                        ) { v ->
                            viewModel.update { it.copy(useDynamicColor = v) }
                        }
                    }
                }
            }

            item(key = "section-calendar") {
                SectionHeader(stringResource(R.string.settings_section_calendar))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_show_lunar),
                        summary = stringResource(R.string.settings_show_lunar_summary),
                        checked = p.showLunar,
                    ) { v ->
                        viewModel.update { it.copy(showLunar = v) }
                    }
                    CardDivider()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(stringResource(R.string.settings_first_day), style = MaterialTheme.typography.bodyLarge)
                        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            days.forEachIndexed { i, day ->
                                SegmentedButton(
                                    selected = p.firstDayOfWeek == day,
                                    onClick = { viewModel.update { it.copy(firstDayOfWeek = day) } },
                                    shape = SegmentedButtonDefaults.itemShape(i, days.size),
                                ) { Text(LichFormat.weekdayFull(day), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }

            item(key = "section-holidays") {
                SectionHeader(stringResource(R.string.settings_section_holidays))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_show_holidays),
                        summary = null,
                        checked = p.showHolidays,
                    ) { v ->
                        viewModel.update { it.copy(showHolidays = v) }
                    }
                    HolidayCategory.entries.forEach { category ->
                        CardDivider()
                        val dotColor = when (category) {
                            HolidayCategory.PUBLIC_HOLIDAY -> MaterialTheme.lich.sunday
                            HolidayCategory.TRADITIONAL -> MaterialTheme.lich.bronze
                            HolidayCategory.OBSERVANCE -> MaterialTheme.lich.festival
                        }
                        SwitchRow(
                            title = LichFormat.holidayCategory(category),
                            summary = null,
                            checked = p.showHolidays && category !in p.hiddenHolidayCategories,
                            enabled = p.showHolidays,
                            leadingDotColor = dotColor,
                        ) { visible ->
                            viewModel.update {
                                it.copy(hiddenHolidayCategories = if (visible) it.hiddenHolidayCategories - category else it.hiddenHolidayCategories + category)
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.settings_holiday_data_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            item(key = "section-reminders") {
                SectionHeader(stringResource(R.string.settings_section_reminders))
                SettingsCard {
                    ValueRow(stringResource(R.string.settings_default_reminder), LichFormat.reminder(p.defaultReminderMinutes, false)) {
                        reminderDialog = false
                    }
                    CardDivider()
                    ValueRow(stringResource(R.string.settings_default_all_day_reminder), LichFormat.reminder(p.defaultAllDayReminderMinutes, true)) {
                        reminderDialog = true
                    }
                    CardDivider()
                    ValueRow(
                        stringResource(R.string.settings_notifications),
                        stringResource(if (notificationsAllowed) R.string.settings_notifications_on else R.string.settings_notifications_off),
                    ) { context.openNotificationSettings() }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CardDivider()
                        ValueRow(
                            stringResource(R.string.settings_exact_alarm),
                            stringResource(if (exactAllowed) R.string.settings_exact_alarm_on else R.string.settings_exact_alarm_off),
                        ) { context.openExactAlarmSettings() }
                    }
                }
            }

            item(key = "section-images") {
                SectionHeader(stringResource(R.string.settings_section_images))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_daily_images),
                        summary = null,
                        checked = p.dailyImagesEnabled,
                    ) { v ->
                        viewModel.update { it.copy(dailyImagesEnabled = v) }
                    }
                    CardDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_wifi_only),
                        summary = stringResource(R.string.settings_wifi_only_summary),
                        checked = p.dailyImagesWifiOnly,
                        enabled = p.dailyImagesEnabled,
                    ) { v ->
                        viewModel.update { it.copy(dailyImagesWifiOnly = v) }
                    }
                    CardDivider()
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
            }

            item(key = "section-about") {
                SectionHeader(stringResource(R.string.settings_section_about))
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                }
                SettingsCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_licenses), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        Text(stringResource(R.string.settings_licenses_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
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
private fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp = 18.dp) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.lich.accent,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    leadingDotColor: Color? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingDotColor != null) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (enabled) leadingDotColor else leadingDotColor.copy(alpha = 0.35f)),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            summary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ValueRow(title: String, value: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        value?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
