package com.app.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.core.domain.repository.NetworkMonitor
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppUiState(val preferences: UserPreferences, val allowNetworkImages: Boolean)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: PreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    /** Null until preferences are read, so the first frame already has the right theme. */
    val state: StateFlow<AppUiState?> = combine(preferences.preferences, networkMonitor.isUnmetered) { prefs, unmetered ->
        AppUiState(prefs, prefs.dailyImagesEnabled && (!prefs.dailyImagesWifiOnly || unmetered))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
