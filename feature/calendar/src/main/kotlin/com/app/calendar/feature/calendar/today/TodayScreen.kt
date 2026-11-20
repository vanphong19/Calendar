package com.app.calendar.feature.calendar.today

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.feature.calendar.DayPaging
import com.app.calendar.feature.calendar.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import com.app.calendar.core.designsystem.R as DsR

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddEvent: (LocalDate) -> Unit,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = DayPaging.pageOf(selected)) { DayPaging.PAGE_COUNT }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Pager -> view model: a settled swipe selects the day (and "tears" the page).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val date = DayPaging.dateOf(page)
            if (date != viewModel.selected.value) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                viewModel.onAction(TodayAction.Select(date))
            }
        }
    }
    // View model -> pager: buttons, date picker, "today".
    LaunchedEffect(selected) {
        val target = DayPaging.pageOf(selected)
        if (target != pagerState.currentPage) {
            if (abs(target - pagerState.currentPage) <= 2) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    val selectedPage by remember(selected) { viewModel.page(selected) }.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val twoPane = maxWidth >= 840.dp
        val pageWidth = maxWidth * 0.5f
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .then(if (twoPane) Modifier.width(pageWidth) else Modifier.fillMaxWidth())
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            ) {
                PageStack(
                    pagerState = pagerState,
                    modifier = Modifier.weight(1f).padding(start = 16.dp, end = 16.dp, top = 8.dp),
                ) { page ->
                    val date = DayPaging.dateOf(page)
                    val state by remember(date) { viewModel.page(date) }.collectAsStateWithLifecycle()
                    TearOffPage(
                        state = state,
                        settings = settings,
                        isToday = date == today,
                        onOpenDetails = { if (!twoPane) showDetails = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ControlBar(
                    isToday = selected == today,
                    onPrevious = { viewModel.onAction(TodayAction.Step(-1)) },
                    onNext = { viewModel.onAction(TodayAction.Step(1)) },
                    onToday = { viewModel.onAction(TodayAction.GoToToday) },
                    onPickDate = { showPicker = true },
                    onAdd = { onAddEvent(selected) },
                )
            }
            if (twoPane) {
                DayDetailContent(
                    state = selectedPage,
                    onAddEvent = { onAddEvent(selected) },
                    onOpenEvent = onOpenEvent,
                    onOpenHoliday = onOpenHoliday,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                        .padding(top = 8.dp),
                )
            }
        }
    }

    if (showDetails) {
        DayDetailSheet(
            onDismiss = { showDetails = false },
            content = {
                DayDetailContent(
                    state = selectedPage,
                    onAddEvent = { showDetails = false; onAddEvent(selected) },
                    onOpenEvent = { showDetails = false; onOpenEvent(it) },
                    onOpenHoliday = { showDetails = false; onOpenHoliday(it) },
                )
            },
        )
    }
    if (showPicker) {
        DayPickerDialog(
            initial = selected,
            onDismiss = { showPicker = false },
            onPicked = { showPicker = false; viewModel.onAction(TodayAction.Select(it)) },
        )
    }
}

/**
 * The block of pages. A leaving page peels off from the top-left corner and flies away while
 * the next page stays put underneath, like tearing a sheet from a Vietnamese lịch bloc.
 * With system animations disabled it degrades to a plain slide.
 */
@Composable
private fun PageStack(
    pagerState: PagerState,
    modifier: Modifier,
    pageContent: @Composable (page: Int) -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val shade = MaterialTheme.lich.paperShade
    val hint = stringResource(R.string.calendar_cd_tear_hint)
    Box(modifier) {
        // Sheets underneath give the block its thickness.
        listOf(2, 1).forEach { depth ->
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = (depth * 3).dp, y = (depth * 4).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shade.copy(alpha = 1f - depth * 0.25f)),
            )
        }
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { it },
            modifier = Modifier.fillMaxSize().semantics { contentDescription = hint },
        ) { page ->
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            Box(
                Modifier
                    .fillMaxSize()
                    // Leaving page on top, current page in the middle, next page underneath.
                    .zIndex(offset)
                    .graphicsLayer {
                        if (reduceMotion) return@graphicsLayer
                        when {
                            // Page being torn off (moving left).
                            offset > 0f -> {
                                // Peel around the top-right corner: the bottom swings up and away to the left.
                                val p = offset.coerceAtMost(1f)
                                transformOrigin = TransformOrigin(1f, 0f)
                                rotationZ = 16f * p
                                translationY = -size.height * 0.06f * p
                                alpha = (1f - p * p).coerceAtLeast(0f)
                                shadowElevation = 10.dp.toPx() * (1f - p)
                            }
                            // Page underneath: cancel the pager's slide so it stays in place.
                            offset < 0f -> translationX = offset * size.width
                        }
                    },
            ) {
                pageContent(page)
            }
        }
    }
}

@Composable
private fun ControlBar(
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickDate: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(DsR.string.action_previous_day))
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(DsR.string.action_next_day))
        }
        if (!isToday) {
            TextButton(onClick = onToday) { Text(stringResource(DsR.string.action_today)) }
        }
        Box(Modifier.weight(1f))
        IconButton(onClick = onPickDate) {
            Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.calendar_pick_date))
        }
        FilledIconButton(onClick = onAdd, modifier = Modifier.padding(start = 4.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(DsR.string.action_add_event))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayDetailSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayPickerDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        yearRange = DayPaging.FIRST.year..DayPaging.LAST.year,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(DsR.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

