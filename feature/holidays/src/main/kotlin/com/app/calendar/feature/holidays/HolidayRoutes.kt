package com.app.calendar.feature.holidays

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HolidaysRoute : NavKey

@Serializable
data class HolidayDetailRoute(val ruleId: String, val year: Int) : NavKey
