package com.app.calendar.feature.calendar.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Tear-off page ("Hôm nay"); [date] is an ISO date to open, or null for today. */
@Serializable
data class TodayRoute(val date: String? = null) : NavKey

/** Wall calendar ("Tháng"). */
@Serializable
data object MonthRoute : NavKey
