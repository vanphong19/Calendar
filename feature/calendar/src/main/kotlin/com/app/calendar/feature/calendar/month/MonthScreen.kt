package com.app.calendar.feature.calendar.month

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.DailyImageView
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.component.EventRow
import com.app.calendar.core.designsystem.component.HolidayRow
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.domain.usecase.AgendaDay
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.feature.calendar.DayPaging
import com.app.calendar.feature.calendar.PageSettings
import com.app.calendar.feature.calendar.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import com.app.calendar.core.designsystem.R as DsR

private val FIRST_MONTH: YearMonth = YearMonth.from(DayPaging.FIRST)
private val MONTH_COUNT: Int = ChronoUnit.MONTHS.between(FIRST_MONTH, YearMonth.from(DayPaging.LAST)).toInt() + 1
private fun monthOf(page: Int): YearMonth = FIRST_MONTH.plusMonths(page.toLong())
private fun pageOf(month: YearMonth): Int = ChronoUnit.MONTHS.between(FIRST_MONTH, month).toInt().coerceIn(0, MONTH_COUNT - 1)

private val WEEK_ANCHOR: LocalDate = LocalDate.of(1901, 1, 7) // a Monday
private const val WEEK_COUNT = 10_400

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthScreen(
    viewModel: MonthViewModel,
    onAddEvent: (LocalDate) -> Unit,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    onOpenPage: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedPage by viewModel.selectedPage.collectAsStateWithLifecycle()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    val monthPager = rememberPagerState(initialPage = pageOf(state.visibleMonth)) { MONTH_COUNT }
    LaunchedEffect(monthPager) {
        snapshotFlow { monthPager.settledPage }.collect { viewModel.onAction(MonthAction.ShowMonth(monthOf(it))) }
    }
    LaunchedEffect(state.visibleMonth) {
        val target = pageOf(state.visibleMonth)
        if (target != monthPager.currentPage) {
            if (kotlin.math.abs(target - monthPager.currentPage) <= 2) monthPager.animateScrollToPage(target) else monthPager.scrollToPage(target)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddEvent(state.selectedDate) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(DsR.string.action_add_event))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "top") {
                TopRow(
                    month = state.visibleMonth,
                    isCurrentMonth = state.visibleMonth == YearMonth.from(today) && state.selectedDate == today,
                    onPickMonth = { showMonthPicker = true },
                    onToday = { viewModel.onAction(MonthAction.GoToToday) },
                )
            }
            item(key = "modes") {
                ModeSelector(state.mode) { viewModel.onAction(MonthAction.SetMode(it)) }
            }
            when (state.mode) {
                CalendarMode.MONTH -> {
                    item(key = "cover") {
                        val cover by remember(state.visibleMonth) { viewModel.cover(state.visibleMonth) }.collectAsStateWithLifecycle()
                        MonthCover(state.visibleMonth, cover, settings)
                    }
                    item(key = "weekdays") { WeekdayHeader(settings.firstDayOfWeek) }
                    item(key = "grid") {
                        HorizontalPager(
                            state = monthPager,
                            key = { it },
                            modifier = Modifier.fillMaxWidth().height(GridRowHeight * 6),
                        ) { page ->
                            val month = monthOf(page)
                            val cells by remember(month) { viewModel.monthCells(month) }.collectAsStateWithLifecycle()
                            MonthGrid(
                                cells = cells,
                                today = today,
                                selected = state.selectedDate,
                                showLunar = settings.showLunar,
                                onSelect = { viewModel.onAction(MonthAction.SelectDate(it)) },
                            )
                        }
                    }
                    item(key = "divider") { HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
                    selectedDayItems(
                        date = state.selectedDate,
                        holidays = selectedPage.details.holidays.takeIf { selectedPage.details.date == state.selectedDate }.orEmpty(),
                        occurrences = selectedPage.details.occurrences.takeIf { selectedPage.details.date == state.selectedDate }.orEmpty(),
                        lunarLabel = { LichFormat.lunarDateLong(selectedPage.details.lunar, selectedPage.details.yearCanChi.displayName) },
                        onAddEvent = onAddEvent,
                        onOpenEvent = onOpenEvent,
                        onOpenHoliday = onOpenHoliday,
                        onOpenPage = onOpenPage,
                    )
                }

                CalendarMode.WEEK -> {
                    item(key = "weekdays") { WeekdayHeader(settings.firstDayOfWeek) }
                    item(key = "week") {
                        WeekStrip(viewModel, state.selectedDate, today, settings) { viewModel.onAction(MonthAction.SelectDate(it)) }
                    }
                    item(key = "divider") { HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
                    selectedDayItems(
                        date = state.selectedDate,
                        holidays = selectedPage.details.holidays.takeIf { selectedPage.details.date == state.selectedDate }.orEmpty(),
                        occurrences = selectedPage.details.occurrences.takeIf { selectedPage.details.date == state.selectedDate }.orEmpty(),
                        lunarLabel = { LichFormat.lunarDateLong(selectedPage.details.lunar, selectedPage.details.yearCanChi.displayName) },
                        onAddEvent = onAddEvent,
                        onOpenEvent = onOpenEvent,
                        onOpenHoliday = onOpenHoliday,
                        onOpenPage = onOpenPage,
                    )
                }

                CalendarMode.AGENDA -> agendaItems(
                    agenda = viewModel.agenda,
                    today = today,
                    onOpenEvent = onOpenEvent,
                    onOpenHoliday = onOpenHoliday,
                    onLoadMore = { viewModel.onAction(MonthAction.LoadMoreAgenda) },
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initial = state.visibleMonth,
            onDismiss = { showMonthPicker = false },
            onPicked = { showMonthPicker = false; viewModel.onAction(MonthAction.ShowMonth(it)) },
        )
    }
}

@Composable
private fun TopRow(month: YearMonth, isCurrentMonth: Boolean, onPickMonth: () -> Unit, onToday: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = stringResource(R.string.calendar_pick_month), onClick = onPickMonth)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(LichFormat.monthYear(month.monthValue, month.year), style = MaterialTheme.typography.headlineSmall)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        Box(Modifier.weight(1f))
        if (!isCurrentMonth) TextButton(onClick = onToday) { Text(stringResource(DsR.string.action_today)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(mode: CalendarMode, onSelect: (CalendarMode) -> Unit) {
    val labels = listOf(
        CalendarMode.MONTH to R.string.calendar_mode_month,
        CalendarMode.WEEK to R.string.calendar_mode_week,
        CalendarMode.AGENDA to R.string.calendar_mode_agenda,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        labels.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/** The picture at the top of a wall-calendar sheet, with the month number printed over it. */
@Composable
private fun MonthCover(month: YearMonth, cover: com.app.calendar.core.model.DailyContent?, settings: PageSettings) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        if (settings.showImages) {
            DailyImageView(cover?.image, settings.allowNetworkImages, cover?.title, Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.lich.paperShade))
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.lich.scrim)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(LichFormat.monthName(month.monthValue).uppercase(), style = LichTextStyles.Band, color = Color.White)
            cover?.title?.takeIf { it.isNotBlank() }?.let {
                Text(listOfNotNull(it, cover.province).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun WeekStrip(
    viewModel: MonthViewModel,
    selected: LocalDate,
    today: LocalDate,
    settings: PageSettings,
    onSelect: (LocalDate) -> Unit,
) {
    val anchor = remember(settings.firstDayOfWeek) { WEEK_ANCHOR.with(java.time.temporal.TemporalAdjusters.previousOrSame(settings.firstDayOfWeek)) }
    fun weekPage(date: LocalDate) = (ChronoUnit.DAYS.between(anchor, date) / 7).toInt().coerceIn(0, WEEK_COUNT - 1)
    val pager = rememberPagerState(initialPage = weekPage(selected)) { WEEK_COUNT }
    var lastSettled by remember { mutableIntStateOf(pager.currentPage) }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            if (page != lastSettled) {
                lastSettled = page
                val weekStart = anchor.plusWeeks(page.toLong())
                onSelect(weekStart.plusDays(selected.dayOfWeek.value.toLong() - settings.firstDayOfWeek.value).let {
                    if (it.isBefore(weekStart)) it.plusWeeks(1) else it
                })
            }
        }
    }
    LaunchedEffect(selected) {
        val target = weekPage(selected)
        if (target != pager.currentPage) {
            lastSettled = target
            pager.scrollToPage(target)
        }
    }
    HorizontalPager(state = pager, key = { it }, modifier = Modifier.fillMaxWidth().height(GridRowHeight)) { page ->
        val weekStart = anchor.plusWeeks(page.toLong())
        val cells by remember(weekStart) { viewModel.weekCells(weekStart) }.collectAsStateWithLifecycle()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            cells.forEach { cell ->
                DayCellView(
                    cell = cell.copy(isInCurrentMonth = true),
                    isToday = cell.date == today,
                    isSelected = cell.date == selected,
                    showLunar = settings.showLunar,
                    onClick = { onSelect(cell.date) },
                    modifier = Modifier.weight(1f).height(GridRowHeight),
                )
            }
        }
    }
}

private fun LazyListScope.selectedDayItems(
    date: LocalDate,
    holidays: List<Holiday>,
    occurrences: List<EventOccurrence>,
    lunarLabel: @Composable () -> String,
    onAddEvent: (LocalDate) -> Unit,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    onOpenPage: (LocalDate) -> Unit,
) {
    item(key = "day-header-$date") {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                Text(
                    LichFormat.weekdayFull(date.dayOfWeek) + ", " + LichFormat.solarDate(date),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(lunarLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onOpenPage(date) }) { Text(stringResource(R.string.calendar_open_page)) }
        }
    }
    items(holidays, key = { "h-${it.ruleId}-${it.date}" }) { holiday ->
        HolidayRow(holiday, Modifier.padding(horizontal = 8.dp), onClick = { onOpenHoliday(holiday) })
    }
    if (occurrences.isEmpty()) {
        item(key = "empty-$date") {
            EmptyState(
                title = stringResource(R.string.calendar_empty_title),
                message = stringResource(R.string.calendar_empty_message),
                actionLabel = stringResource(DsR.string.action_add_event),
                onAction = { onAddEvent(date) },
            )
        }
    } else {
        items(occurrences, key = { "e-${it.event.id}-${it.start}" }) { occurrence ->
            EventRow(occurrence, onClick = { onOpenEvent(occurrence) }, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.agendaItems(
    agenda: kotlinx.coroutines.flow.StateFlow<List<AgendaDay>?>,
    today: LocalDate,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    onLoadMore: () -> Unit,
) {
    item(key = "agenda") {
        val days by agenda.collectAsStateWithLifecycle()
        val list = days
        when {
            list == null -> Text(
                stringResource(DsR.string.loading),
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            list.isEmpty() -> EmptyState(
                title = stringResource(R.string.calendar_agenda_empty_title),
                message = stringResource(R.string.calendar_agenda_empty_message),
            )
            else -> Column {
                list.forEach { day -> AgendaDayBlock(day, isToday = day.date == today, onOpenEvent, onOpenHoliday) }
            }
        }
        if (list != null) {
            TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.calendar_agenda_load_more))
            }
        }
    }
}

@Composable
private fun AgendaDayBlock(day: AgendaDay, isToday: Boolean, onOpenEvent: (EventOccurrence) -> Unit, onOpenHoliday: (Holiday) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Column(
            Modifier.padding(start = 8.dp, top = 8.dp).height(56.dp).semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                LichFormat.weekdayShort(day.date.dayOfWeek),
                style = MaterialTheme.typography.labelMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                day.date.dayOfMonth.toString(),
                style = LichTextStyles.GridDay,
                color = if (isToday) MaterialTheme.colorScheme.primary else com.app.calendar.core.designsystem.component.dayInkColor(day.date, day.holidays.any { it.category == com.app.calendar.core.model.HolidayCategory.PUBLIC_HOLIDAY }),
            )
            Text("%02d/%d".format(day.date.monthValue, day.date.year % 100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            day.holidays.forEach { HolidayRow(it, onClick = { onOpenHoliday(it) }) }
            day.occurrences.forEach { EventRow(it, onClick = { onOpenEvent(it) }) }
        }
    }
}

@Composable
private fun MonthPickerDialog(initial: YearMonth, onDismiss: () -> Unit, onPicked: (YearMonth) -> Unit) {
    var year by rememberSaveable { mutableIntStateOf(initial.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (year > DayPaging.FIRST.year) year-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(DsR.string.action_previous_month))
                }
                Text(year.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { if (year < DayPaging.LAST.year) year++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(DsR.string.action_next_month))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..12).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { m ->
                            val isSelected = initial.year == year && initial.monthValue == m
                            TextButton(
                                onClick = { onPicked(YearMonth.of(year, m)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            ) { Text(LichFormat.monthName(m), textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    )
}
