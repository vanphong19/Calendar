package com.app.calendar.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.app.calendar.core.designsystem.R

/** Be Vietnam Pro: designed for Vietnamese diacritics (SIL Open Font License 1.1). */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_light, FontWeight.Light),
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

internal val LichTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Light),
    displayMedium = base.displayMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Light),
    displaySmall = base.displaySmall.copy(fontFamily = BeVietnamPro),
    headlineLarge = base.headlineLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = BeVietnamPro),
    bodyMedium = base.bodyMedium.copy(fontFamily = BeVietnamPro),
    bodySmall = base.bodySmall.copy(fontFamily = BeVietnamPro),
    labelLarge = base.labelLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium),
)

/** Text styles of the printed-calendar page. Sizes are in sp so they follow the user's font scale. */
object LichTextStyles {
    /** The huge solar day number on a tear-off page. */
    val PageDay = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 156.sp,
        lineHeight = 156.sp,
        letterSpacing = (-0.04).em,
        fontFeatureSettings = "tnum",
    )

    /** Upper-case bands such as "THÁNG MƯỜI MỘT" and "THỨ SÁU". */
    val Band = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.18.em,
    )

    val Weekday = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.12.em,
    )

    /** The lunar day number in the bottom box of the page. */
    val LunarDay = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontFeatureSettings = "tnum",
    )

    val Saying = TextStyle(
        fontFamily = BeVietnamPro,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    /** Day numbers in month grids. */
    val GridDay = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
    )

    val GridLunar = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontFeatureSettings = "tnum",
    )
}
