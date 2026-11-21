package com.app.calendar.feature.calendar.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.app.calendar.core.designsystem.component.DailyImageView
import com.app.calendar.core.designsystem.component.EventColorDot
import com.app.calendar.core.designsystem.component.dayInkColor
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.domain.usecase.HolidayCountdown
import com.app.calendar.core.model.DayDetails
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.OfficialDayOff
import com.app.calendar.feature.calendar.DayPageState
import com.app.calendar.feature.calendar.PageSettings
import com.app.calendar.feature.calendar.R
import com.app.calendar.core.designsystem.R as DsR

/**
 * One sheet of the tear-off calendar (lịch bloc). Layout follows printed Vietnamese calendars:
 * month band, picture, a huge day number, the weekday, the holiday line, a three-column lunar box
 * and a saying at the foot of the page.
 */
@Composable
fun TearOffPage(
    state: DayPageState,
    settings: PageSettings,
    isToday: Boolean,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    countdown: HolidayCountdown? = null,
) {
    val details = state.details
    val colors = MaterialTheme.lich
    Column(
        modifier
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(colors.paper),
    ) {
        BindingStrip()
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val landscapeCompact = maxWidth >= 520.dp && maxHeight < 480.dp
            val roomy = maxHeight >= 560.dp
            val compactWidth = maxWidth < 360.dp
            if (landscapeCompact) {
                val dayNumberHeight = (maxHeight * 0.42f).coerceIn(72.dp, 112.dp)
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .clickable(onClickLabel = stringResource(R.string.calendar_day_details), onClick = onOpenDetails)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PageHeader(details)
                        DayNumber(details, dayNumberHeight, isToday)
                        HolidayLine(details.holidays, details.dayOff)
                    }
                    VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 8.dp), color = colors.ink.copy(alpha = 0.18f))
                    Column(
                        Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (settings.showLunar) LunarBox(details, compactWidth = true)
                        PageFooter(state, countdown, onOpenDetails)
                    }
                }
            } else {
                val dayNumberHeight = (maxHeight * 0.24f).coerceIn(88.dp, 168.dp)
                val content: @Composable ColumnScope.() -> Unit = {
                    PageHeader(details)
                    if (settings.showImages) {
                        PageImage(
                            state = state,
                            allowNetwork = settings.allowNetworkImages,
                            onClick = onOpenDetails,
                            modifier = if (roomy) Modifier.weight(1f).heightIn(min = 120.dp) else Modifier.height(168.dp),
                        )
                    }
                    DayNumber(details, dayNumberHeight, isToday)
                    HolidayLine(details.holidays, details.dayOff)
                    if (settings.showLunar) LunarBox(details, compactWidth = compactWidth)
                    PageFooter(state, countdown, onOpenDetails)
                }
                val columnModifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 14.dp)
                if (roomy) {
                    Column(columnModifier, horizontalAlignment = Alignment.CenterHorizontally, content = content)
                } else {
                    Column(
                        columnModifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }
        }
    }
}

/** Glued binding strip with two ring holes, drawn flat. */
@Composable
private fun BindingStrip() {
    val binding = MaterialTheme.lich.binding
    val hole = MaterialTheme.colorScheme.surface
    Canvas(Modifier.fillMaxWidth().height(18.dp)) {
        drawRect(binding)
        val r = 3.5.dp.toPx()
        listOf(0.3f, 0.7f).forEach { x -> drawCircle(hole, radius = r, center = Offset(size.width * x, size.height / 2)) }
    }
}

@Composable
private fun PageHeader(details: DayDetails) {
    val colors = MaterialTheme.lich
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            LichFormat.monthName(details.date.monthValue).uppercase(),
            style = LichTextStyles.Band,
            color = colors.accent,
            modifier = Modifier.weight(1f),
        )
        Text(details.date.year.toString(), style = LichTextStyles.Band, color = colors.accent)
    }
}

@Composable
private fun PageImage(state: DayPageState, allowNetwork: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val content = state.content
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClickLabel = stringResource(R.string.calendar_day_details), onClick = onClick),
    ) {
        DailyImageView(
            image = content?.image,
            allowNetwork = allowNetwork,
            contentDescription = content?.title,
            modifier = Modifier.fillMaxSize(),
        )
        val label = listOfNotNull(content?.title?.takeIf { it.isNotBlank() }, content?.province).joinToString(" · ")
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.lich.scrim)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DayNumber(details: DayDetails, height: Dp, isToday: Boolean) {
    val ink = dayInkColor(details.date, isDayOff = details.isDayOff())
    val fontSize = with(LocalDensity.current) { (height * 0.92f).toSp() }
    val weekday = LichFormat.weekdayFull(details.date.dayOfWeek)
    val description = buildString {
        append(weekday).append(", ").append(LichFormat.solarDate(details.date))
        if (isToday) append(", ").append(stringResource(R.string.calendar_cd_today))
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = details.date.dayOfMonth.toString(),
            style = LichTextStyles.PageDay.copy(fontSize = fontSize, lineHeight = fontSize),
            color = ink,
        )
        Text(weekday.uppercase(), style = LichTextStyles.Weekday, color = ink)
    }
}

private fun DayDetails.isDayOff(): Boolean =
    dayOff?.kind == OfficialDayOff.Kind.DAY_OFF || holidays.any { it.category == HolidayCategory.PUBLIC_HOLIDAY }

@Composable
private fun HolidayLine(holidays: List<Holiday>, dayOff: OfficialDayOff?) {
    val colors = MaterialTheme.lich
    val lines = holidays.map { holiday ->
        val name = if (holiday.ruleId == "tet-nguyen-dan") stringResource(R.string.calendar_tet_day, holiday.dayIndex) else holiday.name
        name to when (holiday.category) {
            HolidayCategory.PUBLIC_HOLIDAY -> colors.sunday
            HolidayCategory.TRADITIONAL -> colors.bronze
            HolidayCategory.OBSERVANCE -> colors.festival
        }
    } + listOfNotNull(
        dayOff?.takeIf { holidays.none { it.category == HolidayCategory.PUBLIC_HOLIDAY } }?.let {
            val label = if (it.kind == OfficialDayOff.Kind.DAY_OFF) R.string.calendar_day_off else R.string.calendar_make_up_day
            stringResource(label) to if (it.kind == OfficialDayOff.Kind.DAY_OFF) colors.sunday else colors.inkMuted
        },
    )
    if (lines.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        return
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEach { (text, color) ->
            Text(
                text.uppercase(),
                style = LichTextStyles.Band,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/** Three columns like the bottom box of a printed calendar sheet. */
@Composable
private fun LunarBox(details: DayDetails, compactWidth: Boolean = false) {
    val colors = MaterialTheme.lich
    val lunar = details.lunar
    val muted = colors.inkMuted
    val titleStyle = if (compactWidth) MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp) else MaterialTheme.typography.titleSmall
    val bodyStyle = if (compactWidth) MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp) else MaterialTheme.typography.bodySmall
    val centerWidth = if (compactWidth) 68.dp else 90.dp
    HorizontalDivider(color = colors.ink.copy(alpha = 0.18f))
    Row(
        Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 5.dp)) {
            AutoFitSingleLineText(LichFormat.lunarMonth(lunar), style = titleStyle, color = colors.ink, minFontSizeSp = 10.5f)
            AutoFitSingleLineText(
                stringResource(if (lunar.isFullMonth) DsR.string.lunar_month_full else DsR.string.lunar_month_short),
                style = bodyStyle,
                color = muted,
                minFontSizeSp = 10f,
            )
            AutoFitSingleLineText(stringResource(DsR.string.lunar_year, details.yearCanChi.displayName), style = bodyStyle, color = muted, minFontSizeSp = 10f)
        }
        VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 4.dp), color = colors.ink.copy(alpha = 0.18f))
        Column(Modifier.width(centerWidth), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                lunar.day.toString(),
                style = if (compactWidth) LichTextStyles.LunarDay.copy(fontSize = 40.sp, lineHeight = 44.sp) else LichTextStyles.LunarDay,
                color = if (lunar.day == 1 || lunar.day == 15) colors.bronze else colors.ink,
                maxLines = 1,
                softWrap = false,
            )
            Text(stringResource(R.string.calendar_lunar_label).uppercase(), style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1, softWrap = false)
        }
        VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 4.dp), color = colors.ink.copy(alpha = 0.18f))
        Column(Modifier.weight(1f).padding(start = 5.dp), horizontalAlignment = Alignment.End) {
            AutoFitSingleLineText(stringResource(DsR.string.can_chi_day, details.dayCanChi.displayName), style = titleStyle, color = colors.ink, textAlign = TextAlign.End, minFontSizeSp = 10.5f)
            AutoFitSingleLineText(stringResource(DsR.string.can_chi_month, details.monthCanChi.displayName), style = bodyStyle, color = muted, textAlign = TextAlign.End, minFontSizeSp = 10f)
            AutoFitSingleLineText(
                stringResource(if (details.isSolarTermStart) R.string.calendar_solar_term_starts else R.string.calendar_solar_term, details.solarTerm.displayName),
                style = bodyStyle,
                color = muted,
                textAlign = TextAlign.End,
                minFontSizeSp = 9.0f,
            )
        }
    }
    HorizontalDivider(color = colors.ink.copy(alpha = 0.18f))
    Text(
        text = stringResource(R.string.calendar_auspicious_hours) + ": " + details.auspiciousHours.joinToString(", ") { it.displayName },
        style = MaterialTheme.typography.bodySmall,
        color = muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

/** Fits the label during measurement, using ellipsis only when the minimum size cannot fit. */
@Composable
private fun AutoFitSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
    minFontSizeSp: Float,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSizeSp.sp,
            maxFontSize = style.fontSize,
            stepSize = 0.5.sp,
        ),
        modifier = modifier,
    )
}

@Composable
private fun PageFooter(state: DayPageState, countdown: HolidayCountdown?, onOpenDetails: () -> Unit) {
    val colors = MaterialTheme.lich
    val occurrences = state.details.occurrences
    val saying = state.content?.saying
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (saying != null) {
            Text(
                text = "“${saying.text}”",
                style = LichTextStyles.Saying,
                color = colors.ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (countdown != null) CountdownLine(countdown)
        if (occurrences.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = stringResource(R.string.calendar_day_details), onClick = onOpenDetails)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                occurrences.take(2).forEach { occurrence ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventColorDot(occurrence.event.color, Modifier.size(8.dp))
                        Text(
                            LichFormat.occurrenceTime(occurrence) + "  " + occurrence.event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (occurrences.size > 2) {
                    Text(
                        stringResource(R.string.calendar_events_more, occurrences.size - 2),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                    )
                }
            }
        }
    }
}


/** "Còn 78 ngày đến Tết Nguyên Đán", printed like a small stamp at the foot of today's page. */
@Composable
private fun CountdownLine(countdown: HolidayCountdown) {
    val name = countdown.holiday.name
    val text = when (countdown.daysLeft) {
        0L -> stringResource(R.string.calendar_countdown_today, name)
        1L -> stringResource(R.string.calendar_countdown_tomorrow, name)
        else -> stringResource(R.string.calendar_countdown_days, countdown.daysLeft.toInt(), name)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.lich.accent,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.lich.accent.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
