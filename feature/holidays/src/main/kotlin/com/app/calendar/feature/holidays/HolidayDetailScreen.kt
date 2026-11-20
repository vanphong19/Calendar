package com.app.calendar.feature.holidays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.designsystem.component.DailyImageView
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayDetailScreen(viewModel: HolidayDetailViewModel, allowNetworkImages: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {
                    val showTitle = scrollBehavior.state.overlappedFraction > 0.15f
                    if (showTitle) {
                        Text(
                            state.holiday?.name.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(DsR.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
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
        val accent = categoryAccent(holiday.category)

        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val twoPane = maxWidth >= 600.dp
            if (twoPane) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HolidayHeroHeader(state, holiday, accent, allowNetworkImages)
                        DateInfoCard(state, holiday, rule)
                    }
                    Column(
                        Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HolidayBodySections(state, rule)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HolidayHeroHeader(state, holiday, accent, allowNetworkImages)
                    DateInfoCard(state, holiday, rule)
                    HolidayBodySections(state, rule)
                }
            }
        }
    }
}

@Composable
private fun HolidayHeroHeader(
    state: HolidayDetailState,
    holiday: Holiday,
    accent: Color,
    allowNetworkImages: Boolean,
) {
    state.content?.image?.let { image ->
        DailyImageView(
            image = image,
            allowNetwork = allowNetworkImages,
            contentDescription = state.content.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(18.dp)),
            fallbackContent = {
                Column(
                    Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        state.content.title.ifBlank { holiday.name },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        LichFormat.holidayCategory(holiday.category),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accent.copy(alpha = 0.12f),
        ) {
            Text(
                LichFormat.holidayCategory(holiday.category).uppercase(),
                style = LichTextStyles.Band.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = accent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(holiday.name, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun DateInfoCard(
    state: HolidayDetailState,
    holiday: Holiday,
    rule: HolidayRule,
) {
    val dateText = LichFormat.weekdayFull(holiday.date.dayOfWeek) + ", " + LichFormat.solarDate(holiday.date) +
        (state.lastDate?.takeIf { it != holiday.date }?.let { " – " + LichFormat.solarDate(it) } ?: "")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = categoryAccent(holiday.category),
                modifier = Modifier.padding(top = 2.dp).size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(dateText, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
                state.lunar?.let {
                    Text(
                        stringResource(R.string.holidays_lunar_date, it.day, it.month),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        }
    }
}

@Composable
private fun HolidayBodySections(
    state: HolidayDetailState,
    rule: HolidayRule,
) {
    if (rule.description.isNotBlank() || !state.content?.caption.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rule.description.isNotBlank()) {
                Text(rule.description, style = MaterialTheme.typography.bodyLarge)
            }
            state.content?.caption?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (state.officialBreaks.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.holidays_days_off), style = MaterialTheme.typography.titleSmall)
            state.officialBreaks.forEach { br ->
                OfficialBreakCard(
                    officialBreak = br,
                    today = null,
                    showRelativeStatus = false,
                    onClick = null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val sources = state.officialBreaks.map { it.source }.filter { it.isNotBlank() }.distinct()
            if (sources.isNotEmpty()) {
                Text(
                    stringResource(R.string.holidays_source, sources.joinToString("; ")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (state.upcoming.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.holidays_upcoming_years), style = MaterialTheme.typography.titleSmall)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    state.upcoming.forEachIndexed { index, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    item.year.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Text(
                                LichFormat.weekdayFull(item.date.dayOfWeek) + ", " + LichFormat.solarDate(item.date),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (index < state.upcoming.lastIndex) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
