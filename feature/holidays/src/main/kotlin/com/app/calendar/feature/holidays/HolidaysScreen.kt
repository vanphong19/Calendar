package com.app.calendar.feature.holidays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.component.ExpandableSection
import com.app.calendar.core.designsystem.component.dayInkColor
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.domain.usecase.HolidayCountdown
import com.app.calendar.core.lunar.LunarDate
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.OfficialHolidayBreak
import java.time.LocalDate

@Composable
fun HolidaysScreen(viewModel: HolidaysViewModel, onOpenHoliday: (ruleId: String, year: Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = LocalConfiguration.current
    val compactLandscape = config.screenWidthDp >= 560 && config.screenHeightDp < 480

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            YearHeaderBar(
                year = state.year,
                currentYear = state.today.year,
                compact = compactLandscape,
                onPrevious = { viewModel.changeYear(-1) },
                onNext = { viewModel.changeYear(1) },
                onResetCurrentYear = viewModel::resetToCurrentYear,
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wideGrid = maxWidth >= 600.dp
            val listState = rememberLazyListState()
            LaunchedEffect(state.year) { listState.scrollToItem(0) }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            ) {
                item(key = "countdown") {
                    state.countdown?.let { countdown ->
                        CountdownSection(
                            countdown = countdown,
                            lunar = state.countdownLunar,
                            upcoming = state.upcoming,
                            compactLandscape = compactLandscape,
                            onOpenHoliday = { onOpenHoliday(countdown.holiday.ruleId, countdown.holiday.date.year) },
                            onChoose = viewModel::chooseCountdown,
                        )
                    }
                }

                item(key = "official-breaks") {
                    OfficialBreaksSection(
                        year = state.year,
                        today = state.today,
                        breaks = state.officialBreaks,
                        hasOfficialAnnouncement = state.hasOfficialAnnouncement,
                        totalDaysOff = state.totalOfficialDaysOff,
                        twoColumns = wideGrid,
                        onOpenHoliday = { ruleId -> onOpenHoliday(ruleId, state.year) },
                    )
                }

                item(key = "list-header") {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.holidays_list_section),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            state.items?.let { list ->
                                Text(
                                    stringResource(R.string.holidays_list_count, list.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(HolidayCategory.entries) { category ->
                                val dotColor = categoryAccent(category)
                                FilterChip(
                                    selected = category in state.filter,
                                    onClick = { viewModel.toggle(category) },
                                    leadingIcon = {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                                    },
                                    label = {
                                        Text(
                                            LichFormat.holidayCategory(category),
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                val items = state.items
                if (items != null && items.isEmpty()) {
                    item(key = "empty") { EmptyState(title = stringResource(R.string.holidays_empty), message = null) }
                }
                items.orEmpty().groupBy { it.holiday.date.monthValue }.forEach { (month, monthItems) ->
                    item(key = "m-$month") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                LichFormat.monthName(month).uppercase(),
                                style = LichTextStyles.Band,
                                color = MaterialTheme.lich.accent,
                                modifier = Modifier.semantics { heading() },
                            )
                            HorizontalDivider(
                                Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                    items(monthItems, key = { "${it.holiday.ruleId}-${it.holiday.date}" }) { item ->
                        HolidayListRow(item) { onOpenHoliday(item.holiday.ruleId, state.year) }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearHeaderBar(
    year: Int,
    currentYear: Int,
    compact: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetCurrentYear: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.holidays_previous_year))
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.holidays_title, year),
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (year != currentYear) {
                        Surface(
                            onClick = onResetCurrentYear,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Text(
                                stringResource(R.string.holidays_back_to_current_year, currentYear),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.holidays_next_year))
                }
            }
        }
    }
}

/** A tear-off sheet that counts down to the chosen holiday, with a distinct pill selector strip below. */
@Composable
private fun CountdownSection(
    countdown: HolidayCountdown,
    lunar: LunarDate?,
    upcoming: List<Holiday>,
    compactLandscape: Boolean,
    onOpenHoliday: () -> Unit,
    onChoose: (String) -> Unit,
) {
    val colors = MaterialTheme.lich
    val holiday = countdown.holiday
    val description = stringResource(R.string.holidays_countdown_cd, countdown.daysLeft.toInt(), holiday.name)
    val solarText = LichFormat.weekdayFull(holiday.date.dayOfWeek) + ", " + LichFormat.solarDate(holiday.date)
    val lunarText = lunar?.let { stringResource(R.string.holidays_lunar_date, it.day, it.month) }
    val dateLine = listOfNotNull(solarText, lunarText).joinToString(" · ")

    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .clickable(onClickLabel = stringResource(R.string.holidays_open_detail), onClick = onOpenHoliday),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = colors.paper,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                MiniBindingStrip()
                if (compactLandscape) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clearAndSetSemantics { contentDescription = description },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            Modifier.width(116.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (countdown.daysLeft == 0L) {
                                Text(
                                    stringResource(R.string.holidays_countdown_today),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.accent,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text(
                                    countdown.daysLeft.toString(),
                                    style = LichTextStyles.PageDay.copy(fontSize = 52.sp, lineHeight = 56.sp),
                                    color = colors.accent,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                Text(
                                    stringResource(R.string.holidays_countdown_days_short),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.inkMuted,
                                )
                            }
                        }
                        VerticalDivider(
                            Modifier.height(64.dp),
                            color = colors.ink.copy(alpha = 0.14f),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.holidays_countdown_band).uppercase(),
                                style = LichTextStyles.Band.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                color = colors.accent,
                            )
                            Text(
                                holiday.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                dateLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.inkMuted,
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clearAndSetSemantics { contentDescription = description },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.holidays_countdown_band).uppercase(),
                                style = LichTextStyles.Band.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = colors.accent,
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = categoryAccent(holiday.category).copy(alpha = 0.12f),
                            ) {
                                Text(
                                    LichFormat.holidayCategory(holiday.category),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryAccent(holiday.category),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (countdown.daysLeft == 0L) {
                            Text(
                                stringResource(R.string.holidays_countdown_today),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.accent,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        } else {
                            Text(
                                countdown.daysLeft.toString(),
                                style = LichTextStyles.PageDay.copy(fontSize = 84.sp, lineHeight = 88.sp),
                                color = colors.accent,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                stringResource(R.string.holidays_countdown_days_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted,
                            )
                        }
                        Text(
                            holiday.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = colors.inkMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    dateLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            stringResource(R.string.holidays_countdown_choose),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
        )
        val selectedIndex = upcoming.indexOfFirst { it.ruleId == holiday.ruleId }.coerceAtLeast(0)
        val rowState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(upcoming, key = { it.ruleId }) { item ->
                val isSelected = item.ruleId == holiday.ruleId
                val label = stringResource(R.string.holidays_chip_date, item.name, LichFormat.solarDayMonth(item.date))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onChoose(item.ruleId) }
                        .semantics {
                            selected = isSelected
                            role = Role.Button
                        },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBindingStrip() {
    val binding = MaterialTheme.lich.binding
    val hole = MaterialTheme.lich.paper
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        drawRect(binding)
        val r = 2.5.dp.toPx()
        listOf(0.28f, 0.72f).forEach { x ->
            drawCircle(hole, radius = r, center = Offset(size.width * x, size.height / 2f))
        }
    }
}

@Composable
private fun OfficialBreaksSection(
    year: Int,
    today: LocalDate,
    breaks: List<OfficialHolidayBreak>,
    hasOfficialAnnouncement: Boolean,
    totalDaysOff: Int,
    twoColumns: Boolean,
    onOpenHoliday: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.holidays_days_off),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (breaks.isNotEmpty()) {
                val badgeText = if (hasOfficialAnnouncement) {
                    stringResource(R.string.holidays_days_off_summary, breaks.size, totalDaysOff)
                } else {
                    stringResource(R.string.holidays_days_off_statutory_summary, breaks.size)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasOfficialAnnouncement) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasOfficialAnnouncement) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (!hasOfficialAnnouncement) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp).size(18.dp),
                    )
                    Text(
                        stringResource(R.string.holidays_days_off_missing, year),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (twoColumns) {
            breaks.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { officialBreak ->
                        OfficialBreakCard(
                            officialBreak = officialBreak,
                            today = today,
                            showRelativeStatus = year == today.year,
                            onClick = officialBreak.holidayIds.firstOrNull()?.let { id -> { onOpenHoliday(id) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            breaks.forEach { officialBreak ->
                OfficialBreakCard(
                    officialBreak = officialBreak,
                    today = today,
                    showRelativeStatus = year == today.year,
                    onClick = officialBreak.holidayIds.firstOrNull()?.let { id -> { onOpenHoliday(id) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val sources = breaks.map { it.source }.filter { it.isNotBlank() }.distinct()
        if (sources.isNotEmpty()) {
            ExpandableSection(
                title = stringResource(R.string.holidays_sources_title),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                sources.forEach { source ->
                    Text(source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun OfficialBreakCard(
    officialBreak: OfficialHolidayBreak,
    today: LocalDate?,
    showRelativeStatus: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.lich
    val isPast = showRelativeStatus && today != null && officialBreak.endDate.isBefore(today)
    val isOngoing = showRelativeStatus && today != null && !today.isBefore(officialBreak.startDate) && !today.isAfter(officialBreak.endDate)
    val accentColor = when {
        isPast -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        officialBreak.isAnnounced -> colors.sunday
        else -> colors.accent.copy(alpha = 0.85f)
    }
    val dateRangeText = if (officialBreak.startDate == officialBreak.endDate) {
        LichFormat.weekdayShort(officialBreak.startDate.dayOfWeek) + ", " + LichFormat.solarDate(officialBreak.startDate)
    } else {
        stringResource(
            R.string.holidays_range,
            LichFormat.weekdayShort(officialBreak.startDate.dayOfWeek) + ", " + LichFormat.solarDayMonth(officialBreak.startDate),
            LichFormat.weekdayShort(officialBreak.endDate.dayOfWeek) + ", " + LichFormat.solarDate(officialBreak.endDate),
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPast) MaterialTheme.colorScheme.surfaceContainerLow else colors.paper,
        border = BorderStroke(
            width = 1.dp,
            color = if (isOngoing) colors.sunday else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        officialBreak.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when {
                            isPast -> MaterialTheme.colorScheme.surfaceContainerHigh
                            officialBreak.isAnnounced -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                    ) {
                        Text(
                            stringResource(R.string.holidays_days_count, officialBreak.displayDays),
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                                officialBreak.isAnnounced -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                    if (onClick != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        dateRangeText,
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else colors.sunday,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (showRelativeStatus && today != null) {
                        val (statusText, statusColor) = when {
                            isOngoing -> stringResource(R.string.holidays_break_status_ongoing) to colors.sunday
                            isPast -> stringResource(R.string.holidays_break_status_past) to MaterialTheme.colorScheme.onSurfaceVariant
                            else -> stringResource(R.string.holidays_break_status_upcoming) to colors.festival
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Announced cards lead with dates; the full explanation is on their detail screen.
                // Keep statutory caveats visible when no announced schedule exists.
                if ((onClick == null || !officialBreak.isAnnounced) && officialBreak.reason.isNotBlank()) {
                    Text(
                        officialBreak.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                officialBreak.makeUpWorkdays.forEach { makeUp ->
                    val makeUpDate = LichFormat.weekdayShort(makeUp.date.dayOfWeek) + ", " + LichFormat.solarDate(makeUp.date)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isPast) 0.35f else 0.65f),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                stringResource(R.string.holidays_make_up_workday_line, makeUpDate),
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            if (onClick == null) {
                                Text(
                                    makeUp.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolidayListRow(item: HolidayListItem, onClick: () -> Unit) {
    val holiday = item.holiday
    val accent = categoryAccent(holiday.category)
    val dayColor = dayInkColor(holiday.date, holiday.category == HolidayCategory.PUBLIC_HOLIDAY)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 66.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.10f),
            modifier = Modifier.width(52.dp),
        ) {
            Column(
                Modifier.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    holiday.date.dayOfMonth.toString(),
                    style = LichTextStyles.GridDay.copy(fontSize = 21.sp, lineHeight = 24.sp),
                    color = dayColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    LichFormat.weekdayShort(holiday.date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                holiday.name,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildList {
                if (holiday.calendar == CalendarSystem.LUNAR) {
                    add(stringResource(R.string.holidays_lunar_date, item.lunar.day, item.lunar.month))
                }
                if (item.lastDate != holiday.date) {
                    add(stringResource(R.string.holidays_range, LichFormat.solarDayMonth(holiday.date), LichFormat.solarDayMonth(item.lastDate)))
                }
                add(LichFormat.holidayCategory(holiday.category))
            }.joinToString(" · ")
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun categoryAccent(category: HolidayCategory): Color = when (category) {
    HolidayCategory.PUBLIC_HOLIDAY -> MaterialTheme.lich.sunday
    HolidayCategory.TRADITIONAL -> MaterialTheme.lich.bronze
    HolidayCategory.OBSERVANCE -> MaterialTheme.lich.festival
}
