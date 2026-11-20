package com.app.calendar.feature.calendar.month

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.calendar.core.designsystem.component.EventColorDot
import com.app.calendar.core.designsystem.component.dayInkColor
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.DayCell
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate

val GridRowHeight: Dp = 54.dp
private val CellPadding = 2.dp
private val MarkerHeight = 5.dp // EventColorDot is taller than the 3dp holiday marker.

private fun contentPadding(compactHeight: Boolean) = if (compactHeight) 1.dp else 3.dp
private fun markerSpacing(compactHeight: Boolean) = if (compactHeight) 1.dp else 2.dp

@Composable
private fun dayNumberSize(compactHeight: Boolean): Dp = with(LocalDensity.current) {
    maxOf(if (compactHeight) 22.dp else 28.dp, LichTextStyles.GridDay.lineHeight.toDp())
}

@Composable
fun gridRowHeight(showLunar: Boolean, compactHeight: Boolean = false): Dp {
    val lunarHeight = with(LocalDensity.current) {
        if (showLunar) LichTextStyles.GridLunar.lineHeight.toDp() else 0.dp
    }
    // Include both text rows, markers and padding, with room for pixel rounding.
    return CellPadding * 2 + contentPadding(compactHeight) * 2 +
        dayNumberSize(compactHeight) + lunarHeight + markerSpacing(compactHeight) + MarkerHeight + 2.dp
}

@Composable
fun WeekdayHeader(firstDayOfWeek: DayOfWeek, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp).clearAndSetSemantics { }) {
        repeat(7) { index ->
            val day = firstDayOfWeek.plus(index.toLong())
            val color = when (day) {
                DayOfWeek.SUNDAY -> MaterialTheme.lich.sunday
                DayOfWeek.SATURDAY -> MaterialTheme.lich.saturday
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                LichFormat.weekdayShort(day),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Six rows of day cells; a fixed row count keeps the layout from jumping between months. */
@Composable
fun MonthGrid(
    cells: List<DayCell>,
    today: LocalDate,
    selected: LocalDate,
    showLunar: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    compactHeight: Boolean = false,
) {
    val rowHeight = gridRowHeight(showLunar, compactHeight)
    Column(modifier.padding(horizontal = 8.dp)) {
        if (cells.isEmpty()) {
            Spacer(Modifier.height(rowHeight * 6))
            return@Column
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                week.forEach { cell ->
                    DayCellView(
                        cell = cell,
                        isToday = cell.date == today,
                        isSelected = cell.date == selected,
                        showLunar = showLunar,
                        compactHeight = compactHeight,
                        onClick = { onSelect(cell.date) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun DayCellView(
    cell: DayCell,
    isToday: Boolean,
    isSelected: Boolean,
    showLunar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactHeight: Boolean = false,
) {
    val colors = MaterialTheme.lich
    val numberSize = dayNumberSize(compactHeight)
    val ink = dayInkColor(cell.date, cell.isPublicHoliday)
    val numberColor = if (isToday) MaterialTheme.colorScheme.onPrimary else ink
    val background by animateColorAsState(
        if (isSelected && !isToday) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        label = "cellBackground",
    )
    val lunarLabel = LichFormat.lunarGridLabel(cell.lunar)
    val holidayNames = cell.holidays.joinToString(", ") { it.name }
    val description = buildString {
        append(LichFormat.weekdayFull(cell.date.dayOfWeek)).append(", ").append(LichFormat.solarDate(cell.date))
        append(", ").append(stringResource(com.app.calendar.core.designsystem.R.string.lunar_day_month, cell.lunar.day, cell.lunar.month))
        if (isToday) append(", ").append(stringResource(R.string.calendar_cd_today))
        if (cell.eventCount > 0) append(", ").append(stringResource(R.string.calendar_cd_events, cell.eventCount))
        if (holidayNames.isNotEmpty()) append(", ").append(holidayNames)
    }
    Box(
        modifier
            .padding(CellPadding)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (isSelected && isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .selectable(selected = isSelected, onClick = onClick, role = Role.Button)
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
                role = Role.Button
            }
            .alpha(if (cell.isInCurrentMonth) 1f else 0.38f),
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = contentPadding(compactHeight)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(numberSize)
                    .clip(CircleShape)
                    .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cell.date.dayOfMonth.toString(),
                    style = if (compactHeight) LichTextStyles.GridDay.copy(fontSize = LichTextStyles.GridDay.fontSize * 0.8f) else LichTextStyles.GridDay,
                    color = numberColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (showLunar) {
                val lunarColor = when {
                    cell.lunar.day == 1 || cell.lunar.day == 15 -> colors.bronze
                    else -> colors.inkMuted
                }
                Text(lunarLabel, style = LichTextStyles.GridLunar, color = lunarColor, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(markerSpacing(compactHeight)))
            Row(
                Modifier.height(MarkerHeight),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val holidayColor = cell.holidays.firstOrNull()?.let {
                    when (it.category) {
                        HolidayCategory.PUBLIC_HOLIDAY -> colors.sunday
                        HolidayCategory.TRADITIONAL -> colors.bronze
                        HolidayCategory.OBSERVANCE -> colors.festival
                    }
                }
                if (holidayColor != null) Box(Modifier.size(width = 10.dp, height = 3.dp).clip(CircleShape).background(holidayColor))
                cell.eventColors.forEach { EventColorDot(it) }
            }
        }
    }
}

