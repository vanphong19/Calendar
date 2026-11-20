package com.app.calendar.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.app.calendar.core.designsystem.theme.LocalLichColors
import java.time.DayOfWeek
import java.time.LocalDate

/** Printed-calendar convention: Sundays and days off in red, Saturdays in blue, other days in ink. */
@Composable
@ReadOnlyComposable
fun dayInkColor(date: LocalDate, isDayOff: Boolean): Color {
    val colors = LocalLichColors.current
    return when {
        isDayOff || date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        else -> colors.ink
    }
}
