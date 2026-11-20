package com.app.calendar.core.data.repository

import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.domain.repository.HolidayRepository
import com.app.calendar.core.model.CalendarSystem
import com.app.calendar.core.model.HolidayCategory
import com.app.calendar.core.model.HolidayRule
import com.app.calendar.core.model.OfficialDayOff
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holiday rules and official day-off schedules bundled as JSON (assets/holidays). The format is
 * the same one a remote source would serve, so the data can later be updated without an app release.
 */
@Singleton
internal class AssetHolidayRepository @Inject constructor(
    private val assets: AssetReader,
) : HolidayRepository {

    private val mutex = Mutex()
    private var rules: List<HolidayRule>? = null

    override suspend fun rules(): List<HolidayRule> = mutex.withLock {
        rules ?: parseRules(assets.readTextOrNull(RULES_PATH).orEmpty()).also { rules = it }
    }

    override suspend fun officialDaysOff(year: Int): List<OfficialDayOff> {
        val text = assets.readTextOrNull("holidays/days-off/$year.json") ?: return emptyList()
        return parseDaysOff(text)
    }

    internal fun parseRules(text: String): List<HolidayRule> {
        if (text.isBlank()) return emptyList()
        return assets.json.decodeFromString<HolidayRulesDto>(text).rules.map { dto ->
            HolidayRule(
                id = dto.id,
                name = dto.name,
                category = HolidayCategory.valueOf(dto.category),
                calendar = CalendarSystem.valueOf(dto.calendar),
                month = dto.month,
                day = dto.day,
                lastDayOfMonth = dto.lastDayOfMonth,
                durationDays = dto.durationDays,
                description = dto.description,
                sinceYear = dto.sinceYear,
            )
        }
    }

    internal fun parseDaysOff(text: String): List<OfficialDayOff> =
        assets.json.decodeFromString<DaysOffDto>(text).entries.flatMap { entry ->
            val from = LocalDate.parse(entry.from)
            val to = LocalDate.parse(entry.to ?: entry.from)
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { date ->
                OfficialDayOff(
                    date = date,
                    kind = OfficialDayOff.Kind.valueOf(entry.kind),
                    reason = entry.reason,
                    source = entry.source,
                    title = entry.title,
                    holidayIds = entry.holidayIds,
                )
            }.toList()
        }

    private companion object {
        const val RULES_PATH = "holidays/rules.json"
    }
}

@Serializable
private data class HolidayRulesDto(val version: Int, val rules: List<HolidayRuleDto>)

@Serializable
private data class HolidayRuleDto(
    val id: String,
    val name: String,
    val category: String,
    val calendar: String,
    val month: Int,
    val day: Int = 0,
    val lastDayOfMonth: Boolean = false,
    val durationDays: Int = 1,
    val description: String = "",
    val sinceYear: Int? = null,
)

@Serializable
private data class DaysOffDto(val year: Int, val entries: List<DayOffEntryDto>)

@Serializable
private data class DayOffEntryDto(
    val from: String,
    val to: String? = null,
    val kind: String = "DAY_OFF",
    val title: String = "",
    val holidayIds: List<String> = emptyList(),
    val reason: String,
    val source: String,
)
