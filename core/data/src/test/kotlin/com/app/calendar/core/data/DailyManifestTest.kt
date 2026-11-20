package com.app.calendar.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.data.repository.AssetDailyContentRepository
import com.app.calendar.core.data.repository.AssetHolidayRepository
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
    private val repository = AssetDailyContentRepository(reader)
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

    private fun assertWithId(id: String, condition: Boolean) = assertWithMessage(id).that(condition).isTrue()
}
