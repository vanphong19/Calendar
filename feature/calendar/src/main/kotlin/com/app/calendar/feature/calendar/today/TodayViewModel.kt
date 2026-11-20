package com.app.calendar.feature.calendar.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.calendar.feature.calendar.DayPageLoader
import com.app.calendar.feature.calendar.DayPageState
import com.app.calendar.feature.calendar.PageSettings
import com.app.calendar.feature.calendar.asState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

sealed interface TodayAction {
    data class Select(val date: LocalDate) : TodayAction
    data object GoToToday : TodayAction
    data class Step(val days: Long) : TodayAction
}

@HiltViewModel(assistedFactory = TodayViewModel.Factory::class)
class TodayViewModel @AssistedInject constructor(
    @Assisted initialDate: LocalDate?,
    private val loader: DayPageLoader,
) : ViewModel() {

    val today: StateFlow<LocalDate> = loader.today().asState(viewModelScope, LocalDate.now())

    private val _selected = MutableStateFlow(initialDate ?: LocalDate.now())
    /** Single source of truth for the page shown; the pager follows it. */
    val selected: StateFlow<LocalDate> = _selected.asStateFlow()

    val settings: StateFlow<PageSettings> = loader.settings(viewModelScope)

    fun page(date: LocalDate): StateFlow<DayPageState> = loader.page(date, viewModelScope)

    fun onAction(action: TodayAction) {
        when (action) {
            is TodayAction.Select -> _selected.value = action.date
            TodayAction.GoToToday -> _selected.value = today.value
            is TodayAction.Step -> _selected.value = _selected.value.plusDays(action.days)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialDate: LocalDate?): TodayViewModel
    }
}
