package com.app.calendar.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.domain.repository.ReminderScheduler
import com.app.calendar.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    val state: StateFlow<UserPreferences?> =
        preferences.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferences.update(transform) }
    }

    /** Called when the user returns from the system exact-alarm screen. */
    fun onAlarmPermissionChanged() = reminderScheduler.requestReschedule()
}
