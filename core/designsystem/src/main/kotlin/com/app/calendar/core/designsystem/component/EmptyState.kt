package com.app.calendar.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.calendar.core.designsystem.theme.lich

/** Quiet empty state: a small line drawing of a calendar page, a message and an optional action. */
@Composable
fun EmptyState(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.lich.binding
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(56.dp)) {
            val w = size.width
            val stroke = Stroke(width = 2.dp.toPx())
            drawRoundRect(line, topLeft = Offset(w * 0.15f, w * 0.18f), size = Size(w * 0.7f, w * 0.72f), cornerRadius = CornerRadius(6.dp.toPx()), style = stroke)
            drawRect(accent, topLeft = Offset(w * 0.15f, w * 0.18f), size = Size(w * 0.7f, w * 0.12f))
            drawLine(line, Offset(w * 0.3f, w * 0.52f), Offset(w * 0.7f, w * 0.52f), strokeWidth = 2.dp.toPx())
            drawLine(line, Offset(w * 0.3f, w * 0.66f), Offset(w * 0.56f, w * 0.66f), strokeWidth = 2.dp.toPx())
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
