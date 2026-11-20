package com.app.calendar.core.model

import java.time.LocalDate

/** The image, story and saying shown on the tear-off page of a given day. */
data class DailyContent(
    val date: LocalDate,
    val title: String,
    val caption: String,
    val province: String?,
    val image: DailyImage?,
    val saying: Saying?,
)

data class DailyImage(
    /** Remote, cacheable URL (may be null when only a bundled asset exists). */
    val remoteUrl: String?,
    /** Path inside the APK assets used as offline fallback. */
    val localAsset: String?,
    val author: String,
    val license: String,
    val sourceUrl: String,
    /** ARGB colour used as placeholder while loading and as background when offline. */
    val dominantColor: Long,
)

/** Ca dao, tục ngữ or a public-domain quotation, with an optional short explanation. */
data class Saying(val text: String, val source: String?, val note: String?)
