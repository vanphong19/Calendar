package com.app.calendar.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.app.calendar.core.model.EventColor

// Palette: sơn mài (lacquer red), lúa non (young rice), đồng (bronze), giấy dó (rice paper), mực tàu (ink).

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFB3362B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF3F6B4E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC1EDCB),
    onSecondaryContainer = Color(0xFF002110),
    tertiary = Color(0xFF8A6A1E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA1),
    onTertiaryContainer = Color(0xFF2A1F00),
    background = Color(0xFFFBF7F1),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFBF7F1),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE6DA),
    onSurfaceVariant = Color(0xFF4E4539),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1E9),
    surfaceContainer = Color(0xFFF2EBE2),
    surfaceContainerHigh = Color(0xFFECE5DB),
    surfaceContainerHighest = Color(0xFFE6DFD5),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD9CDBE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A6),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFF8F1F17),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFA6D1B0),
    onSecondary = Color(0xFF0F3820),
    secondaryContainer = Color(0xFF275139),
    onSecondaryContainer = Color(0xFFC1EDCB),
    tertiary = Color(0xFFE7C26C),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5E4600),
    onTertiaryContainer = Color(0xFFFFDEA1),
    background = Color(0xFF16130F),
    onBackground = Color(0xFFEAE1D7),
    surface = Color(0xFF16130F),
    onSurface = Color(0xFFEAE1D7),
    surfaceVariant = Color(0xFF4E4539),
    onSurfaceVariant = Color(0xFFD3C5B4),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF231F1A),
    surfaceContainerHigh = Color(0xFF2E2924),
    surfaceContainerHighest = Color(0xFF39342E),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4E4539),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Colours specific to the paper-calendar look, kept consistent with Vietnamese printed calendars. */
@Immutable
data class LichColors(
    /** The tear-off page itself. */
    val paper: Color,
    /** Pages stacked behind the current one. */
    val paperShade: Color,
    /** Glued binding strip at the top of the block. */
    val binding: Color,
    /** Accent for headings printed on paper (month band, section titles); readable in both themes. */
    val accent: Color,
    /** Weekday numerals. */
    val ink: Color,
    val inkMuted: Color,
    /** Sundays and official days off (in đỏ). */
    val sunday: Color,
    /** Saturdays (in xanh). */
    val saturday: Color,
    /** Mùng 1 and rằm. */
    val bronze: Color,
    /** Traditional and commemorative days. */
    val festival: Color,
    val scrim: Color,
    private val events: Map<EventColor, Color>,
) {
    fun event(color: EventColor): Color = events.getValue(color)
}

internal val LightLichColors = LichColors(
    paper = Color(0xFFFFFCF6),
    paperShade = Color(0xFFEDE3D3),
    binding = Color(0xFFB3362B),
    accent = Color(0xFFB3362B),
    ink = Color(0xFF1D1A17),
    inkMuted = Color(0xFF6B6157),
    sunday = Color(0xFFC0322A),
    saturday = Color(0xFF2D5B8C),
    bronze = Color(0xFF8A6A1E),
    festival = Color(0xFF3F6B4E),
    scrim = Color(0x99000000),
    events = mapOf(
        EventColor.LACQUER to Color(0xFFC0392B),
        EventColor.RICE to Color(0xFF4C8C5A),
        EventColor.APRICOT to Color(0xFFC98A12),
        EventColor.RIVER to Color(0xFF2F6FA8),
        EventColor.LOTUS to Color(0xFFC2577E),
        EventColor.INK to Color(0xFF5B5F66),
    ),
)

internal val DarkLichColors = LichColors(
    paper = Color(0xFF221E19),
    paperShade = Color(0xFF2E2923),
    binding = Color(0xFF8F1F17),
    accent = Color(0xFFFF9C8F),
    ink = Color(0xFFEDE4D9),
    inkMuted = Color(0xFFB3A797),
    sunday = Color(0xFFFF8A7C),
    saturday = Color(0xFF9CC3F0),
    bronze = Color(0xFFE7C26C),
    festival = Color(0xFFA6D1B0),
    scrim = Color(0xB3000000),
    events = mapOf(
        EventColor.LACQUER to Color(0xFFFF8A7C),
        EventColor.RICE to Color(0xFF9BD3A6),
        EventColor.APRICOT to Color(0xFFF2C66B),
        EventColor.RIVER to Color(0xFF94C1EE),
        EventColor.LOTUS to Color(0xFFF4A6C1),
        EventColor.INK to Color(0xFFBFC3CA),
    ),
)

val LocalLichColors = staticCompositionLocalOf { LightLichColors }
