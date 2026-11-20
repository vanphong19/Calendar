package com.app.calendar.core.domain.holiday

import com.app.calendar.core.model.Holiday
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.OfficialDayOff
import com.app.calendar.core.model.OfficialHolidayBreak
import kotlin.math.abs

/**
 * Builds the chronological list of official public-holiday breaks for [yearData].
 *
 * When a year has an announced official schedule ([YearHolidays.daysOff]), consecutive `DAY_OFF`
 * entries sharing the same title/reason are merged into one [OfficialHolidayBreak] and their
 * corresponding `MAKE_UP_WORKDAY` entries are attached directly to that break.
 *
 * When a year has no announced schedule yet (e.g. future years), statutory `PUBLIC_HOLIDAY` dates
 * are synthesized into baseline breaks (merging consecutive 30/4 and 1/5) with `isAnnounced = false`.
 */
fun buildOfficialBreaks(yearData: YearHolidays): List<OfficialHolidayBreak> {
    if (yearData.daysOff.isEmpty()) {
        return buildStatutoryBaseline(yearData.holidays)
    }

    val sorted = yearData.daysOff.values.sortedBy { it.date }
    val dayOffEntries = sorted.filter { it.kind == OfficialDayOff.Kind.DAY_OFF }
    val makeUps = sorted.filter { it.kind == OfficialDayOff.Kind.MAKE_UP_WORKDAY }

    val groups = mutableListOf<MutableList<OfficialDayOff>>()
    dayOffEntries.forEach { day ->
        val last = groups.lastOrNull()?.lastOrNull()
        val sameGroup = last != null &&
            last.date.plusDays(1) == day.date &&
            last.reason == day.reason &&
            last.title == day.title &&
            last.holidayIds == day.holidayIds
        if (sameGroup) groups.last().add(day) else groups.add(mutableListOf(day))
    }

    val claimedMakeUps = mutableSetOf<OfficialDayOff>()
    val breaks = groups.map { group ->
        val first = group.first()
        val last = group.last()
        val matchingMakeUps = makeUps.filter { makeUp ->
            makeUp !in claimedMakeUps && matchesBreak(makeUp, first, last)
        }
        claimedMakeUps.addAll(matchingMakeUps)
        val fallbackTitle = first.title.ifBlank {
            val matchedNames = yearData.holidays
                .filter { it.category == HolidayCategory.PUBLIC_HOLIDAY && !it.date.isBefore(first.date) && !it.date.isAfter(last.date) }
                .map { it.name }
                .distinct()
            matchedNames.joinToString(" & ").ifBlank { first.reason }
        }
        OfficialHolidayBreak(
            id = "${first.date}-${last.date}",
            title = fallbackTitle,
            startDate = first.date,
            endDate = last.date,
            reason = first.reason,
            source = first.source,
            holidayIds = first.holidayIds.distinct(),
            makeUpWorkdays = matchingMakeUps.sortedBy { it.date },
            isAnnounced = true,
        )
    }
    return breaks
}

/**
 * Returns the official breaks relevant to [ruleId] in [yearData].
 * Uses explicit [OfficialHolidayBreak.holidayIds] when present; falls back to ±7 day proximity
 * only for legacy entries without explicit holiday links.
 */
fun breaksForHoliday(ruleId: String, yearData: YearHolidays): List<OfficialHolidayBreak> {
    if (yearData.daysOff.isEmpty()) return emptyList()
    val breaks = buildOfficialBreaks(yearData)
    val explicit = breaks.filter { ruleId in it.holidayIds }
    if (explicit.isNotEmpty()) return explicit

    val holidayDates = yearData.holidays.filter { it.ruleId == ruleId }.map { it.date }
    if (holidayDates.isEmpty()) return emptyList()
    val firstDate = holidayDates.first()
    return breaks.filter { br ->
        br.holidayIds.isEmpty() && (
            abs(br.startDate.toEpochDay() - firstDate.toEpochDay()) <= 7 ||
                abs(br.endDate.toEpochDay() - firstDate.toEpochDay()) <= 7
            )
    }
}

private fun matchesBreak(
    makeUp: OfficialDayOff,
    first: OfficialDayOff,
    last: OfficialDayOff,
): Boolean {
    if (makeUp.holidayIds.isNotEmpty() && first.holidayIds.isNotEmpty()) {
        return makeUp.holidayIds.any { it in first.holidayIds }
    }
    if (makeUp.title.isNotBlank() && first.title.isNotBlank()) {
        return makeUp.title == first.title
    }
    val distStart = abs(makeUp.date.toEpochDay() - first.date.toEpochDay())
    val distEnd = abs(makeUp.date.toEpochDay() - last.date.toEpochDay())
    return minOf(distStart, distEnd) <= 14
}

private fun buildStatutoryBaseline(holidays: List<Holiday>): List<OfficialHolidayBreak> {
    val publicHolidays = holidays
        .filter { it.category == HolidayCategory.PUBLIC_HOLIDAY }
        .sortedBy { it.date }
    if (publicHolidays.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<Holiday>>()
    publicHolidays.forEach { holiday ->
        val last = groups.lastOrNull()?.lastOrNull()
        val consecutive = last != null && (last.date == holiday.date || last.date.plusDays(1) == holiday.date)
        if (consecutive) groups.last().add(holiday) else groups.add(mutableListOf(holiday))
    }

    return groups.map { group ->
        val first = group.first()
        val last = group.last()
        val ids = group.map { it.ruleId }.distinct()
        val title = when {
            ids.containsAll(listOf("giai-phong-mien-nam", "quoc-te-lao-dong")) ->
                "Ngày 30/4 & Quốc tế Lao động"
            else -> group.map { it.name }.distinct().joinToString(" & ")
        }
        val reason = when {
            "tet-duong-lich" in ids ->
                "Nghỉ 1 ngày hưởng nguyên lương theo Bộ luật Lao động"
            "tet-nguyen-dan" in ids ->
                "Nghỉ 5 ngày theo Bộ luật Lao động (lịch cụ thể công bố hằng năm)"
            "gio-to-hung-vuong" in ids ->
                "Nghỉ 1 ngày hưởng nguyên lương (10/3 âm lịch)"
            ids.containsAll(listOf("giai-phong-mien-nam", "quoc-te-lao-dong")) ->
                "Nghỉ lễ 30/4 và Quốc tế Lao động 1/5 theo Bộ luật Lao động"
            "quoc-khanh" in ids ->
                "Nghỉ 2 ngày theo Bộ luật Lao động (ngày 2/9 và 1 ngày liền kề)"
            "van-hoa-viet-nam" in ids ->
                "Nghỉ 1 ngày hưởng nguyên lương (Nghị quyết 28/2026/QH16)"
            else -> first.description.substringBefore('.').trim().ifBlank { first.name }
        }
        val statutoryDays = when {
            "tet-nguyen-dan" in ids -> 5
            "quoc-khanh" in ids -> 2
            else -> (last.date.toEpochDay() - first.date.toEpochDay()).toInt() + 1
        }
        OfficialHolidayBreak(
            id = "statutory-${ids.joinToString("_")}-${first.date}",
            title = title,
            startDate = first.date,
            endDate = last.date,
            reason = reason,
            source = "Bộ luật Lao động 2019, Điều 112; Nghị quyết 28/2026/QH16",
            holidayIds = ids,
            makeUpWorkdays = emptyList(),
            isAnnounced = false,
            displayDays = statutoryDays,
        )
    }
}
