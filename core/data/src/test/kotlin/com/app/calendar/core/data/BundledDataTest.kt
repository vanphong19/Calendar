package com.app.calendar.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.data.repository.AssetHolidayRepository
import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.lunar.LunarCalendar
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.OfficialDayOff
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Validates the JSON shipped in assets/holidays. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BundledDataTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = AssetHolidayRepository(AssetReader(context, Dispatchers.Unconfined))
    private val lunar = LunarCalendar()

    @Test
    fun `holiday rules parse and include the statutory holidays`() = runTest {
        val rules = repository.rules()
        assertThat(rules.map { it.id }.toSet()).hasSize(rules.size)
        assertThat(rules.map { it.id }).containsAtLeast(
            "tet-duong-lich", "tet-nguyen-dan", "gio-to-hung-vuong", "giai-phong-mien-nam", "van-hoa-viet-nam",
            "quoc-te-lao-dong", "quoc-khanh", "quoc-te-phu-nu", "phu-nu-viet-nam", "nha-giao", "trung-thu",
        )
        rules.forEach { rule ->
            assertThat(rule.month).isIn(1..12)
            if (!rule.lastDayOfMonth) assertThat(rule.day).isIn(1..31)
        }
    }

    @Test
    fun `lunar holidays resolve to the expected 2026 dates`() = runTest {
        val holidays = HolidayCalendar.resolve(repository.rules(), 2026, lunar).associateBy { it.ruleId to it.dayIndex }
        assertThat(holidays.getValue("tet-nguyen-dan" to 1).date).isEqualTo(LocalDate.of(2026, 2, 17))
        assertThat(holidays.getValue("giao-thua" to 1).date).isEqualTo(LocalDate.of(2026, 2, 16))
        assertThat(holidays.getValue("gio-to-hung-vuong" to 1).date).isEqualTo(LocalDate.of(2026, 4, 26))
        assertThat(holidays.getValue("gio-to-hung-vuong" to 1).calendar).isEqualTo(CalendarSystem.LUNAR)
    }

    @Test
    fun `official days off for 2026 parse with all six breaks, make-up workdays and sources`() = runTest {
        val daysOff = repository.officialDaysOff(2026)
        val offDates = daysOff.filter { it.kind == OfficialDayOff.Kind.DAY_OFF }.map { it.date }
        val makeUpDates = daysOff.filter { it.kind == OfficialDayOff.Kind.MAKE_UP_WORKDAY }.map { it.date }
        assertThat(offDates).containsAtLeast(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 2, 14), LocalDate.of(2026, 2, 22),
            LocalDate.of(2026, 4, 25), LocalDate.of(2026, 4, 27),
            LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 3),
            LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 11, 24),
        )
        assertThat(makeUpDates).containsExactly(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 8, 22))
        assertThat(daysOff.all { it.source.isNotBlank() && it.title.isNotBlank() && it.holidayIds.isNotEmpty() }).isTrue()
        val ruleIds = repository.rules().map { it.id }.toSet()
        daysOff.flatMap { it.holidayIds }.forEach { assertThat(ruleIds).contains(it) }
        assertThat(repository.officialDaysOff(1999)).isEmpty()
    }
}
