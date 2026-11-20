package com.app.calendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.calendar.core.data.reminder.ReminderNotifier
import com.app.calendar.core.designsystem.theme.LichTheme
import com.app.calendar.navigation.LichApp
import dagger.hilt.android.AndroidEntryPoint

/** Request to open a specific event, e.g. from a reminder notification. */
data class OpenEventRequest(val eventId: Long, val date: String?)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingOpen by mutableStateOf<OpenEventRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingOpen = intent.toOpenRequest()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val app = state ?: return@setContent
            LichTheme(themeMode = app.preferences.themeMode, dynamicColor = app.preferences.useDynamicColor) {
                LichApp(
                    allowNetworkImages = app.allowNetworkImages,
                    openRequest = pendingOpen,
                    onOpenRequestHandled = { pendingOpen = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toOpenRequest()?.let { pendingOpen = it }
    }

    private fun Intent.toOpenRequest(): OpenEventRequest? {
        val id = getLongExtra(ReminderNotifier.EXTRA_OPEN_EVENT_ID, -1)
        return if (id > 0) OpenEventRequest(id, getStringExtra(ReminderNotifier.EXTRA_OPEN_DATE)) else null
    }
}
