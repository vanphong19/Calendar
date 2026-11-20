package com.app.calendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.calendar.core.designsystem.format.LichFormat
import com.app.calendar.core.designsystem.theme.lich
import com.app.calendar.core.model.EventColor
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory

/** One event in a list: colour bar, time and title. Flat, no card. */
@Composable
fun EventRow(occurrence: EventOccurrence, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val event = occurrence.event
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.lich.event(event.color)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                add(LichFormat.occurrenceTime(occurrence))
                if (event.location.isNotBlank()) add(event.location)
            }.joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A holiday line in day lists. */
@Composable
fun HolidayRow(holiday: Holiday, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val color = when (holiday.category) {
        HolidayCategory.PUBLIC_HOLIDAY -> MaterialTheme.lich.sunday
        HolidayCategory.TRADITIONAL -> MaterialTheme.lich.bronze
        HolidayCategory.OBSERVANCE -> MaterialTheme.lich.festival
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f)) {
            Text(holiday.name, style = MaterialTheme.typography.titleSmall, color = color)
            Text(
                LichFormat.holidayCategory(holiday.category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EventColorDot(color: EventColor, modifier: Modifier = Modifier) {
    Box(modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.lich.event(color)))
}
