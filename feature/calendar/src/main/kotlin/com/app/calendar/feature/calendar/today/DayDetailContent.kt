package com.app.calendar.feature.calendar.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.calendar.core.designsystem.component.EmptyState
import com.app.calendar.core.designsystem.component.EventRow
import com.app.calendar.core.designsystem.component.HolidayRow
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.LichTextStyles
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.feature.calendar.DayPageState
import com.app.calendar.feature.calendar.R
import com.app.calendar.core.designsystem.R as DsR

/** Full information about a day: events, holidays, the picture's story, the saying and lunar data. */
@Composable
fun DayDetailContent(
    state: DayPageState,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        dayDetailItems(state, onOpenEvent, onOpenHoliday)
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.dayDetailItems(
    state: DayPageState,
    onOpenEvent: (EventOccurrence) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
) {
    val details = state.details
    item(key = "header") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                LichFormat.weekdayFull(details.date.dayOfWeek) + ", " + LichFormat.solarDate(details.date),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                LichFormat.lunarDateLong(details.lunar, details.yearCanChi.displayName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (details.holidays.isNotEmpty()) {
        item(key = "holidays-title") { SectionTitle(stringResource(R.string.calendar_sheet_holidays)) }
        items(details.holidays, key = { "h-" + it.ruleId + it.dayIndex }) { holiday ->
            HolidayRow(holiday, Modifier.padding(horizontal = 8.dp), onClick = { onOpenHoliday(holiday) })
        }
    }
    item(key = "events-title") { SectionTitle(stringResource(R.string.calendar_sheet_events)) }
    if (details.occurrences.isEmpty()) {
        item(key = "events-empty") {
            EmptyState(
                title = stringResource(R.string.calendar_empty_title),
                message = stringResource(R.string.calendar_empty_message),
            )
        }
    } else {
        items(details.occurrences, key = { "e-" + it.event.id + it.start }) { occurrence ->
            EventRow(occurrence, onClick = { onOpenEvent(occurrence) }, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
    state.content?.let { content ->
        if (content.title.isNotBlank() || content.caption.isNotBlank()) {
            item(key = "image") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(stringResource(R.string.calendar_sheet_image), Modifier)
                    Text(
                        listOfNotNull(content.title, content.province).joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (content.caption.isNotBlank()) Text(content.caption, style = MaterialTheme.typography.bodyMedium)
                    content.image?.let { image ->
                        val uriHandler = LocalUriHandler.current
                        Text(
                            stringResource(DsR.string.image_credit, image.author, image.license),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = { runCatching { uriHandler.openUri(image.sourceUrl) } },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(R.string.calendar_sheet_image_source))
                        }
                    }
                }
            }
        }
        content.saying?.let { saying ->
            item(key = "saying") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(stringResource(R.string.calendar_sheet_saying), Modifier)
                    Text("“${saying.text}”", style = LichTextStyles.Saying)
                    saying.source?.let { Text("— $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    saying.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
    item(key = "lunar") {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.calendar_sheet_lunar), Modifier)
            Text(
                listOf(
                    stringResource(DsR.string.can_chi_day, details.dayCanChi.displayName),
                    stringResource(DsR.string.can_chi_month, details.monthCanChi.displayName),
                    stringResource(DsR.string.can_chi_year, details.yearCanChi.displayName),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(if (details.isSolarTermStart) R.string.calendar_solar_term_starts else R.string.calendar_solar_term, details.solarTerm.displayName),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.calendar_auspicious_hours), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.lich.bronze)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                details.auspiciousHours.forEach { chi ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            stringResource(R.string.calendar_auspicious_hour_range, chi.displayName, chi.startHour, chi.endHour),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier.padding(horizontal = 20.dp)) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.lich.accent,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}
