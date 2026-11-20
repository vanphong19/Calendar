package com.app.calendar.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.app.calendar.core.model.ThemeMode

@Composable
fun LichTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    // The paper-calendar colours stay on-brand even with dynamic colour, so red Sundays remain red.
    CompositionLocalProvider(LocalLichColors provides if (dark) DarkLichColors else LightLichColors) {
        // Material 3 Expressive (MaterialExpressiveTheme + MotionScheme.expressive) becomes public in
        // material3 1.5, which needs compileSdk 37.1; switch here once that SDK is installed.
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LichTypography,
            content = content,
        )
    }
}

object LichThemeExt {
    val colors: LichColors
        @Composable @ReadOnlyComposable get() = LocalLichColors.current
}

/** Shorthand: `MaterialTheme.lich.sunday`. */
val MaterialTheme.lich: LichColors
    @Composable @ReadOnlyComposable get() = LocalLichColors.current
