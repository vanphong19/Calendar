package com.app.calendar.core.lunar

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Low-level astronomical formulas (Jean Meeus, "Astronomical Algorithms", 1998) used to build the
 * Vietnamese lunisolar calendar. All day numbers are Julian Day Numbers (JDN, integer days).
 */
internal object Astronomy {
    private const val DR = PI / 180.0

    /** Mean length of a synodic month in days. */
    const val SYNODIC_MONTH = 29.530588853

    /** JD (fractional) of the reference new moon of 1900-01-01 (k = 0). */
    const val NEW_MOON_EPOCH = 2415021.076998695

    /** Julian day (fractional, UT) of the k-th new moon after the epoch. */
    fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * DR)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * DR) + 0.0021 * sin(2 * DR * m)
        c1 = c1 - 0.4068 * sin(mpr * DR) + 0.0161 * sin(DR * 2 * mpr)
        c1 -= 0.0004 * sin(DR * 3 * mpr)
        c1 = c1 + 0.0104 * sin(DR * 2 * f) - 0.0051 * sin(DR * (m + mpr))
        c1 = c1 - 0.0074 * sin(DR * (m - mpr)) + 0.0004 * sin(DR * (2 * f + m))
        c1 = c1 - 0.0004 * sin(DR * (2 * f - m)) - 0.0006 * sin(DR * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(DR * (2 * f - mpr)) + 0.0005 * sin(DR * (2 * mpr + m))
        val deltaT = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltaT
    }

    /** Local day number (JDN) on which the k-th new moon occurs for the given UTC offset (hours). */
    fun newMoonDay(k: Int, timeZone: Double): Int = floor(newMoon(k) + 0.5 + timeZone / 24.0).toInt()

    /** Apparent solar longitude in radians, normalised to [0, 2π), at fractional Julian day [jd]. */
    fun sunLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val t2 = t * t
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(DR * m)
        dl += (0.019993 - 0.000101 * t) * sin(DR * 2 * m) + 0.000290 * sin(DR * 3 * m)
        var l = (l0 + dl) * DR
        l -= PI * 2 * floor(l / (PI * 2))
        return l
    }

    /** Sun longitude at local midnight starting [dayNumber], bucketed into [segments] equal arcs. */
    fun sunLongitudeSegment(dayNumber: Int, timeZone: Double, segments: Int): Int =
        floor(sunLongitude(dayNumber - 0.5 - timeZone / 24.0) / (2 * PI) * segments).toInt()
}
