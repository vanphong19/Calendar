package com.app.calendar.feature.events.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Create (eventId == null, on [date]) or edit an event. */
@Serializable
data class EventEditorRoute(val eventId: Long? = null, val date: String? = null) : NavKey

/** Details of one occurrence of an event. */
@Serializable
data class EventDetailRoute(val eventId: Long, val occurrenceDate: String? = null) : NavKey

/** "Sự kiện" tab: the place to add and manage personal events. */
@Serializable
data object EventsRoute : NavKey
