package com.app.calendar.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.data.repository.AssetDailyContentRepository
import com.app.calendar.core.data.repository.AssetHolidayRepository
import com.app.calendar.core.model.dailyContentRuleIds
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/** Guards the curated daily-content dataset (assets/daily/manifest.json). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val reader = AssetReader(context, Dispatchers.Unconfined)
    private val lunar = com.app.calendar.core.lunar.LunarCalendar()
    private val repository = AssetDailyContentRepository(reader, lunar)
    private val manifest = repository.parse(context.assets.open("daily/manifest.json").bufferedReader().readText())

    private val allowedLicense = Regex("^(CC0( 1\\.0)?|Public domain|PD.*|CC BY(-SA)? [1-4]\\.0( .*)?|CC BY(-SA)? 2\\.5( .*)?)$", RegexOption.IGNORE_CASE)

    @Test
    fun `every month cover has a bundled offline image`() {
        (1..12).forEach { month ->
            val entry = manifest.months["%02d".format(month)]
            assertThat(entry).isNotNull()
            val image = manifest.images.getValue(checkNotNull(entry).image!!)
            assertThat(image.asset).isNotNull()
            context.assets.open(image.asset!!).use { assertThat(it.available()).isGreaterThan(0) }
        }
    }

    @Test
    fun `every holiday rule has dedicated content`() = runTest {
        val ids = AssetHolidayRepository(reader).rules().map { it.id }
        assertThat(manifest.holidays.keys).containsAtLeastElementsIn(ids)
    }

    @Test
    fun `references, licenses and bundled files are valid`() {
        val referenced = (manifest.days.values + manifest.holidays.values + manifest.months.values).mapNotNull { it.image }.toSet()
        assertThat(manifest.images.keys).containsAtLeastElementsIn(referenced)
        manifest.images.forEach { (id, image) ->
            assertWithId(id, allowedLicense.matches(image.license.trim()))
            assertWithId(id, image.author.isNotBlank() && image.source.startsWith("https://"))
            assertWithId(id, image.remote != null || image.asset != null)
            image.asset?.let { path -> context.assets.open(path).use { assertWithId(id, it.available() > 0) } }
        }
    }

    @Test
    fun `day entries use valid keys and never repeat an image`() {
        val validKeys = generateSequence(LocalDate.of(2024, 1, 1)) { it.plusDays(1) }.take(366)
            .map { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }.toSet()
        assertThat(validKeys).containsAtLeastElementsIn(manifest.days.keys)
        val images = manifest.days.values.mapNotNull { it.image }
        assertThat(images.toSet().size).isEqualTo(images.size)
    }

    @Test
    fun `content is stable for a date and covers leap day`() = runTest {
        val a = repository.contentFor(LocalDate.of(2028, 2, 29), emptyList())
        val b = repository.contentFor(LocalDate.of(2028, 2, 29), emptyList())
        assertThat(a).isEqualTo(b)
        assertThat(a.title).isNotEmpty()
        assertThat(repository.monthCover(YearMonth.of(2026, 1)).image).isNotNull()
        // A lunar holiday picks its own entry whatever the Gregorian date.
        val tet = repository.contentFor(LocalDate.of(2027, 2, 6), listOf("tet-nguyen-dan"))
        assertThat(tet.title).isEqualTo(manifest.holidays.getValue("tet-nguyen-dan").title)
    }

    @Test
    fun `sayings pool is large enough and every saying is explained`() {
        assertThat(manifest.sayings.size).isAtLeast(100)
        manifest.sayings.forEach { assertThat(it.text).isNotEmpty() }
    }

    @Test
    fun `tet multi day period uses different images for each day`() = runTest {
        val holidays = com.app.calendar.core.domain.holiday.HolidayCalendar(
            AssetHolidayRepository(reader), lunar,
        ).year(2026)
        val ids = (0L..2L).map { offset ->
            val date = LocalDate.of(2026, 2, 17).plusDays(offset)
            repository.contentFor(date, holidays.holidaysOn(date).dailyContentRuleIds()).image?.sourceUrl
        }
        assertThat(ids).doesNotContain(null)
        assertThat(ids.toSet()).hasSize(3)
    }

    @Test
    fun `pool sayings always match the lunar season of the day`() {
        var date = LocalDate.of(2026, 1, 1)
        val seasonalSeen = mutableSetOf<String>()
        while (date.year < 2028) {
            val saying = repository.poolSaying(manifest.sayings, date)!!
            val month = lunar.toLunar(date).month
            assertWithMessage("$date: ${saying.text}").that(saying.lunarMonths.isEmpty() || month in saying.lunarMonths).isTrue()
            if (saying.lunarMonths.isNotEmpty()) seasonalSeen += saying.text
            date = date.plusDays(1)
        }
        assertThat(seasonalSeen).isNotEmpty()
    }

    @Test
    fun `every month tag is a valid lunar month and day entries avoid movable lunar dates`() {
        manifest.sayings.flatMap { it.lunarMonths }.forEach { assertThat(it).isIn(1..12) }
        // A fixed Gregorian day cannot carry a saying tied to a lunar month: it would be wrong in some years.
        manifest.days.forEach { (key, entry) ->
            entry.saying?.let { assertWithMessage(key).that(it.lunarMonths).isEmpty() }
        }
    }

    @Test
    fun `every place appears once and all 63 former provinces are covered`() {
        val entries = manifest.days.values + manifest.holidays.values + manifest.months.values
        val places = entries.map { it.place }
        assertWithMessage("entries without place").that(places.filter { it.isNullOrBlank() }).isEmpty()
        val repeated = places.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertWithMessage("places used more than once").that(repeated).isEmpty()

        // Check the underlying file as well: different slugs must not hide the same photo.
        val imageIds = entries.mapNotNull { it.image }
        assertWithMessage("images used more than once").that(imageIds.toSet()).hasSize(imageIds.size)

        // Known aliases and commonly confused places need an explicit canonical identity.
        val aliases = mapOf("con-lan" to "cu-lao-thoi-son")
        val canonicalPlaces = places.mapNotNull { it }.map { aliases[it] ?: it }
        assertWithMessage("same place under different names").that(canonicalPlaces.toSet()).hasSize(canonicalPlaces.size)

        val perProvince = manifest.days.values.groupingBy { it.province }.eachCount()
        assertWithMessage("unknown province names").that(perProvince.keys - FORMER_PROVINCES).isEmpty()
        FORMER_PROVINCES.forEach { province ->
            assertWithMessage("days for $province").that(perProvince[province] ?: 0).isAtLeast(MIN_DAYS_PER_PROVINCE)
            assertWithMessage("days for $province").that(perProvince[province] ?: 0).isAtMost(MAX_DAYS_PER_PROVINCE)
        }
    }

    private fun assertWithId(id: String, condition: Boolean) = assertWithMessage(id).that(condition).isTrue()

    private companion object {
        const val MIN_DAYS_PER_PROVINCE = 3
        const val MAX_DAYS_PER_PROVINCE = 12

        /** The 63 provinces and cities before the 2025 merger, spelled as shown in the app. */
        val FORMER_PROVINCES = setOf(
            "Hà Nội", "TP. Hồ Chí Minh", "Hải Phòng", "Đà Nẵng", "Cần Thơ",
            "An Giang", "Bà Rịa – Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh", "Bến Tre",
            "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau", "Cao Bằng", "Đắk Lắk",
            "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh",
            "Hải Dương", "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum",
            "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định", "Nghệ An", "Ninh Bình",
            "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh",
            "Quảng Trị", "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa",
            "Thừa Thiên Huế", "Tiền Giang", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái",
        )
    }
}
