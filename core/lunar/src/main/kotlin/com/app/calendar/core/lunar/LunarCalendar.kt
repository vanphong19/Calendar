package com.app.calendar.core.lunar

import java.time.LocalDate
import kotlin.math.floor

/**
 * Vietnamese lunisolar calendar conversions.
 *
 * Built on the astronomical method popularised by Hồ Ngọc Đức: months begin on the local day of
 * the new moon, the month containing the winter solstice is month 11, and in a 13-month year the
 * first month without a major solar term is the leap month. Vietnam uses UTC+7 ([VIETNAM_TIME_ZONE]),
 * which is why some years differ from the Chinese calendar (e.g. Tết 1985 and 2007).
 *
 * Results are cached per "solstice window" (lunar month 11 of one year to month 11 of the next),
 * so rendering a month grid costs a handful of map lookups. Thread-safe.
 */
class LunarCalendar(private val timeZone: Double = VIETNAM_TIME_ZONE) {

    private val windowCache = object : LinkedHashMap<Int, List<LunarMonth>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<LunarMonth>>?) = size > MAX_CACHED_WINDOWS
    }

    /** Converts a Gregorian date to its lunar date. */
    fun toLunar(date: LocalDate): LunarDate {
        val jdn = date.toJdn()
        var window = windowStartingIn(date.year)
        if (jdn < window.first().startJdn) window = windowStartingIn(date.year - 1)
        val month = window.last { it.startJdn <= jdn }
        return LunarDate(
            day = jdn - month.startJdn + 1,
            month = month.month,
            year = month.year,
            isLeapMonth = month.isLeap,
            monthLength = month.length,
        )
    }

    /**
     * Converts a lunar date to Gregorian, or returns null when the date does not exist
     * (e.g. a leap month that the year does not have, or day 30 of a 29-day month).
     */
    fun toSolar(day: Int, month: Int, year: Int, isLeapMonth: Boolean = false): LocalDate? {
        val lunarMonth = findMonth(month, year, isLeapMonth) ?: return null
        if (day !in 1..lunarMonth.length) return null
        return jdnToDate(lunarMonth.startJdn + day - 1)
    }

    fun toSolar(lunar: LunarDate): LocalDate? = toSolar(lunar.day, lunar.month, lunar.year, lunar.isLeapMonth)

    /** The lunar month (month, leap) of [year], or null if it does not exist. */
    fun findMonth(month: Int, year: Int, isLeap: Boolean = false): LunarMonth? {
        require(month in 1..12) { "Lunar month must be 1..12, was $month" }
        val windowYear = if (month >= 11) year else year - 1
        return windowStartingIn(windowYear).firstOrNull { it.month == month && it.year == year && it.isLeap == isLeap }
    }

    /** All months (12 or 13) of a lunar year in order, from tháng Giêng to tháng Chạp. */
    fun monthsOfYear(year: Int): List<LunarMonth> =
        windowStartingIn(year - 1).filter { it.year == year } + windowStartingIn(year).filter { it.year == year }

    /** The leap month number of a lunar year, or null for a regular 12-month year. */
    fun leapMonthOf(year: Int): Int? = monthsOfYear(year).firstOrNull { it.isLeap }?.month

    /** Can chi of a day. */
    fun dayCanChi(date: LocalDate): CanChi {
        val jdn = date.toJdn()
        return canChiOf(jdn + 9, jdn + 1)
    }

    /** Can chi of a lunar month (a leap month shares the name of the regular month). */
    fun monthCanChi(month: Int, year: Int): CanChi = canChiOf(year * 12 + month + 3, month + 1)

    /** Can chi of a lunar year, e.g. 2026 -> Bính Ngọ. */
    fun yearCanChi(year: Int): CanChi = canChiOf(year + 6, year + 8)

    /** Can of the first double-hour (giờ Tý) of the given day. */
    fun firstHourCan(date: LocalDate): Can = Can.entries[Math.floorMod((date.toJdn() - 1) * 2, 10)]

    /** Giờ hoàng đạo: the six auspicious double-hours of the day, in clock order starting at Tý. */
    fun auspiciousHours(date: LocalDate): List<Chi> {
        val chiOfDay = Math.floorMod(date.toJdn() + 1, 12)
        val pattern = AUSPICIOUS_HOUR_PATTERNS[chiOfDay % 6]
        return Chi.entries.filterIndexed { index, _ -> pattern[index] == '1' }
    }

    /** The solar term (tiết khí) in effect at the end of the given day. */
    fun solarTerm(date: LocalDate): SolarTerm =
        SolarTerm.entries[Astronomy.sunLongitudeSegment(date.toJdn() + 1, timeZone, 24)]

    /** True when a new solar term begins on this day. */
    fun isSolarTermStart(date: LocalDate): Boolean = solarTerm(date) != solarTerm(date.minusDays(1))

    // --- internals -------------------------------------------------------------------------

    /** Months from lunar month 11 of [gregorianYear] (inclusive) to month 11 of the next year (exclusive). */
    private fun windowStartingIn(gregorianYear: Int): List<LunarMonth> {
        synchronized(windowCache) { windowCache[gregorianYear]?.let { return it } }
        val computed = computeWindow(gregorianYear)
        synchronized(windowCache) { windowCache[gregorianYear] = computed }
        return computed
    }

    private fun computeWindow(gregorianYear: Int): List<LunarMonth> {
        val a11 = lunarMonth11(gregorianYear)
        val b11 = lunarMonth11(gregorianYear + 1)
        val k = floor(0.5 + (a11 - Astronomy.NEW_MOON_EPOCH) / Astronomy.SYNODIC_MONTH).toInt()
        val monthCount = if (b11 - a11 > 365) 13 else 12
        val leapOffset = if (monthCount == 13) leapMonthOffset(a11) else -1

        val starts = IntArray(monthCount + 1) { i ->
            when (i) {
                0 -> a11
                monthCount -> b11
                else -> Astronomy.newMoonDay(k + i, timeZone)
            }
        }
        var seenFirstMonth = false
        return List(monthCount) { i ->
            val isLeap = i == leapOffset
            val ordinal = if (leapOffset in 0..i) i - 1 else i // months elapsed since month 11, ignoring the leap
            val month = (10 + ordinal) % 12 + 1
            if (month == 1 && !isLeap) seenFirstMonth = true
            LunarMonth(
                month = month,
                year = if (seenFirstMonth) gregorianYear + 1 else gregorianYear,
                isLeap = isLeap,
                startJdn = starts[i],
                length = starts[i + 1] - starts[i],
            )
        }
    }

    /** Day number of the start of the lunar month containing the winter solstice of [year]. */
    private fun lunarMonth11(year: Int): Int {
        val offset = LocalDate.of(year, 12, 31).toJdn() - 2415021
        val k = floor(offset / Astronomy.SYNODIC_MONTH).toInt()
        val newMoon = Astronomy.newMoonDay(k, timeZone)
        val sunLongitude = Astronomy.sunLongitudeSegment(newMoon, timeZone, 12)
        return if (sunLongitude >= 9) Astronomy.newMoonDay(k - 1, timeZone) else newMoon
    }

    /** Index (from month 11 = 0) of the leap month in a 13-month window starting at [a11]. */
    private fun leapMonthOffset(a11: Int): Int {
        val k = floor((a11 - Astronomy.NEW_MOON_EPOCH) / Astronomy.SYNODIC_MONTH + 0.5).toInt()
        var i = 1
        var arc = Astronomy.sunLongitudeSegment(Astronomy.newMoonDay(k + i, timeZone), timeZone, 12)
        var last: Int
        do {
            last = arc
            i++
            arc = Astronomy.sunLongitudeSegment(Astronomy.newMoonDay(k + i, timeZone), timeZone, 12)
        } while (arc != last && i < 14)
        return i - 1
    }

    companion object {
        const val VIETNAM_TIME_ZONE = 7.0
        private const val MAX_CACHED_WINDOWS = 24
        private const val JDN_OF_EPOCH_DAY_0 = 2440588

        /** Indexed by (chi of day) % 6; character i = '1' when branch i is a hoàng đạo hour. */
        private val AUSPICIOUS_HOUR_PATTERNS = arrayOf(
            "110100101100", "001101001011", "110011010010",
            "101100110100", "001011001101", "010010110011",
        )

        fun LocalDate.toJdn(): Int = (toEpochDay() + JDN_OF_EPOCH_DAY_0).toInt()
        fun jdnToDate(jdn: Int): LocalDate = LocalDate.ofEpochDay((jdn - JDN_OF_EPOCH_DAY_0).toLong())
    }
}
