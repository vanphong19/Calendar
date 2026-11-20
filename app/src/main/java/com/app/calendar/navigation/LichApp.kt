package com.app.calendar.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.app.calendar.OpenEventRequest
import com.app.calendar.core.designsystem.R as DsR
import com.app.calendar.core.model.EventOccurrence
import com.app.calendar.core.model.Holiday
import com.app.calendar.feature.calendar.month.MonthScreen
import com.app.calendar.feature.calendar.month.MonthViewModel
import com.app.calendar.feature.calendar.navigation.MonthRoute
import com.app.calendar.feature.calendar.navigation.TodayRoute
import com.app.calendar.feature.calendar.today.TodayScreen
import com.app.calendar.feature.calendar.today.TodayViewModel
import com.app.calendar.feature.events.detail.EventDetailScreen
import com.app.calendar.feature.events.detail.EventDetailViewModel
import com.app.calendar.feature.events.editor.EventEditorScreen
import com.app.calendar.feature.events.editor.EventEditorViewModel
import com.app.calendar.feature.events.navigation.EventDetailRoute
import com.app.calendar.feature.events.navigation.EventEditorRoute
import com.app.calendar.feature.events.navigation.EventsRoute
import com.app.calendar.feature.events.list.EventsScreen
import com.app.calendar.feature.events.list.EventsViewModel
import com.app.calendar.feature.holidays.HolidayDetailRoute
import com.app.calendar.feature.holidays.HolidayDetailScreen
import com.app.calendar.feature.holidays.HolidayDetailViewModel
import com.app.calendar.feature.holidays.HolidaysRoute
import com.app.calendar.feature.holidays.HolidaysScreen
import com.app.calendar.feature.holidays.HolidaysViewModel
import com.app.calendar.feature.settings.SettingsRoute
import com.app.calendar.feature.settings.SettingsScreen
import com.app.calendar.feature.settings.SettingsViewModel
import java.time.LocalDate

private enum class TopLevel(val route: NavKey, val icon: ImageVector, val label: Int) {
    TODAY(TodayRoute(), Icons.Filled.Home, DsR.string.nav_today),
    MONTH(MonthRoute, Icons.Filled.DateRange, DsR.string.nav_month),
    EVENTS(EventsRoute, Icons.AutoMirrored.Filled.List, DsR.string.nav_events),
    HOLIDAYS(HolidaysRoute, Icons.Filled.Star, DsR.string.nav_holidays),
    SETTINGS(SettingsRoute, Icons.Filled.Settings, DsR.string.nav_settings),
}

private fun NavKey.topLevel(): TopLevel? = when (this) {
    is TodayRoute -> TopLevel.TODAY
    MonthRoute -> TopLevel.MONTH
    EventsRoute -> TopLevel.EVENTS
    HolidaysRoute -> TopLevel.HOLIDAYS
    SettingsRoute -> TopLevel.SETTINGS
    else -> null
}

/** Root of the UI: adaptive navigation (bar on phones, rail on tablets) around a Navigation 3 back stack. */
@Composable
fun LichApp(
    allowNetworkImages: Boolean,
    openRequest: OpenEventRequest?,
    onOpenRequestHandled: () -> Unit,
) {
    val backStack = rememberNavBackStack(TodayRoute())

    LaunchedEffect(openRequest) {
        openRequest?.let {
            backStack.add(EventDetailRoute(it.eventId, it.date))
            onOpenRequestHandled()
        }
    }

    val current = backStack.lastOrNull()
    val selectedTab = backStack.lastOrNull { it.topLevel() != null }?.topLevel() ?: TopLevel.TODAY
    val showNavigation = current?.topLevel() != null

    val openEvent: (EventOccurrence) -> Unit = { backStack.add(EventDetailRoute(it.event.id, it.date.toString())) }
    val addEvent: (LocalDate) -> Unit = { backStack.add(EventEditorRoute(date = it.toString())) }
    val openHoliday: (Holiday) -> Unit = { backStack.add(HolidayDetailRoute(it.ruleId, it.date.year)) }

    val itemColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        ),
    )
    NavigationSuiteScaffold(
        navigationSuiteColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.colors(
            navigationBarContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
            navigationRailContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        navigationSuiteItems = {
            TopLevel.entries.forEach { tab ->
                item(
                    selected = tab == selectedTab,
                    onClick = { backStack.switchTo(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label), maxLines = 1) },
                    colors = itemColors,
                )
            }
        },
        layoutType = if (showNavigation) {
            androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                androidx.compose.material3.adaptive.currentWindowAdaptiveInfo(),
            )
        } else {
            androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.None
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayRoute> { route ->
                    val vm = hiltViewModel<TodayViewModel, TodayViewModel.Factory>(
                        creationCallback = { it.create(route.date?.let(LocalDate::parse)) },
                    )
                    TodayScreen(vm, onOpenEvent = openEvent, onOpenHoliday = openHoliday)
                }
                entry<MonthRoute> {
                    MonthScreen(
                        viewModel = hiltViewModel<MonthViewModel>(),
                        onOpenEvent = openEvent,
                        onOpenHoliday = openHoliday,
                        onOpenPage = { date -> backStack.switchTo(TodayRoute(date.toString())) },
                    )
                }
                entry<EventsRoute> {
                    EventsScreen(hiltViewModel<EventsViewModel>(), onAddEvent = addEvent, onOpenEvent = openEvent)
                }
                entry<HolidaysRoute> {
                    HolidaysScreen(hiltViewModel<HolidaysViewModel>()) { ruleId, year -> backStack.add(HolidayDetailRoute(ruleId, year)) }
                }
                entry<SettingsRoute> { SettingsScreen(hiltViewModel<SettingsViewModel>()) }
                entry<EventEditorRoute> { route ->
                    val vm = hiltViewModel<EventEditorViewModel, EventEditorViewModel.Factory>(
                        creationCallback = { it.create(route.eventId, route.date?.let(LocalDate::parse)) },
                    )
                    EventEditorScreen(
                        viewModel = vm,
                        onClose = { backStack.removeLastOrNull() },
                        onSaved = { backStack.removeLastOrNull() },
                    )
                }
                entry<EventDetailRoute> { route ->
                    val vm = hiltViewModel<EventDetailViewModel, EventDetailViewModel.Factory>(
                        creationCallback = { it.create(route.eventId, route.occurrenceDate?.let(LocalDate::parse)) },
                    )
                    EventDetailScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() },
                        onEdit = { id -> backStack.add(EventEditorRoute(eventId = id)) },
                    )
                }
                entry<HolidayDetailRoute> { route ->
                    val vm = hiltViewModel<HolidayDetailViewModel, HolidayDetailViewModel.Factory>(
                        creationCallback = { it.create(route.ruleId, route.year) },
                    )
                    HolidayDetailScreen(vm, allowNetworkImages) { backStack.removeLastOrNull() }
                }
            },
        )
    }
}

/** Tabs replace the whole stack so back from a tab exits, like most calendar apps. */
private fun NavBackStack<NavKey>.switchTo(route: NavKey) {
    if (size == 1 && first() == route) return
    clear()
    add(route)
}
