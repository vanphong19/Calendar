package com.app.calendar.core.data.repository

import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.DailyContent
import com.app.calendar.core.model.DailyImage
import com.app.calendar.core.model.Saying
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily images and stories from a versioned manifest (assets/daily/manifest.json).
 *
 * Selection is deterministic: holiday entry → "MM-dd" entry → the month's cover. Sayings fall back
 * to a pool indexed by day-of-year, so the same date always shows the same content.
 */
@Singleton
internal class AssetDailyContentRepository @Inject constructor(
    private val assets: AssetReader,
    private val lunarCalendar: LunarCalendar,
) : DailyContentRepository {

    private val mutex = Mutex()
    private var manifest: DailyManifestDto? = null

    private suspend fun manifest(): DailyManifestDto = mutex.withLock {
        manifest ?: (assets.readTextOrNull(MANIFEST_PATH)?.let { parse(it) } ?: DailyManifestDto()).also { manifest = it }
    }

    internal fun parse(text: String): DailyManifestDto = assets.json.decodeFromString(text)

    override suspend fun contentFor(date: LocalDate, holidayRuleIds: List<String>): DailyContent {
        val m = manifest()
        val entry = holidayRuleIds.firstNotNullOfOrNull { m.holidays[it] }
            ?: m.days[dayKey(date)]
            ?: m.months[monthKey(date.monthValue)]
        val saying = entry?.saying ?: poolSaying(m.sayings, date)
        return DailyContent(
            date = date,
            title = entry?.title.orEmpty(),
            caption = entry?.caption.orEmpty(),
            province = entry?.province,
            image = entry?.image?.let { m.images[it] }?.toModel(),
            saying = saying?.toModel(),
        )
    }

    override suspend fun monthCover(month: YearMonth): DailyContent {
        val m = manifest()
        val entry = m.months[monthKey(month.monthValue)]
        return DailyContent(
            date = month.atDay(1),
            title = entry?.title.orEmpty(),
            caption = entry?.caption.orEmpty(),
            province = entry?.province,
            image = entry?.image?.let { m.images[it] }?.toModel(),
            saying = entry?.saying?.toModel(),
        )
    }

    /**
     * Picks a saying from the pool that fits the season. Sayings that mention a month refer to the
     * lunar month ("tháng tám" = tháng Tám âm lịch), so they are only eligible during those lunar
     * months; timeless sayings are eligible every day. Deterministic for a given date.
     */
    internal fun poolSaying(pool: List<SayingDto>, date: LocalDate): SayingDto? {
        if (pool.isEmpty()) return null
        val lunarMonth = lunarCalendar.toLunar(date).month
        val candidates = pool.filter { it.lunarMonths.isEmpty() || lunarMonth in it.lunarMonths }
        return candidates[(date.dayOfYear - 1) % candidates.size]
    }

    private fun dayKey(date: LocalDate) = "%02d-%02d".format(date.monthValue, date.dayOfMonth)
    private fun monthKey(month: Int) = "%02d".format(month)

    private companion object {
        const val MANIFEST_PATH = "daily/manifest.json"
    }
}

@Serializable
internal data class DailyManifestDto(
    val version: Int = 0,
    val images: Map<String, ImageDto> = emptyMap(),
    val days: Map<String, EntryDto> = emptyMap(),
    val holidays: Map<String, EntryDto> = emptyMap(),
    val months: Map<String, EntryDto> = emptyMap(),
    val sayings: List<SayingDto> = emptyList(),
)

@Serializable
internal data class EntryDto(
    val title: String,
    val caption: String = "",
    val province: String? = null,
    val image: String? = null,
    val saying: SayingDto? = null,
)

@Serializable
internal data class ImageDto(
    val remote: String? = null,
    val asset: String? = null,
    val author: String,
    val license: String,
    val source: String,
    val color: String = "#8C7B6B",
) {
    fun toModel() = DailyImage(
        remoteUrl = remote,
        localAsset = asset,
        author = author,
        license = license,
        sourceUrl = source,
        dominantColor = parseColor(color),
    )

    private fun parseColor(hex: String): Long = runCatching { 0xFF000000L or hex.removePrefix("#").toLong(16) }.getOrDefault(0xFF8C7B6BL)
}

@Serializable
internal data class SayingDto(
    val text: String,
    val source: String? = null,
    val note: String? = null,
    /** Lunar months (1–12) the saying belongs to; empty = fits any time of year. */
    val lunarMonths: List<Int> = emptyList(),
) {
    fun toModel() = Saying(text, source, note)
}
