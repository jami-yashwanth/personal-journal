package com.personal.journal

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JournalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            runCatching { container.repository.startupCleanup() }
            runCatching {
                container.applyReminder(container.settings.reminderEnabled.first(), container.settings.reminderTime.first(), replace = false)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> container.lockGate.onBackground()
                    Lifecycle.Event.ON_START -> container.lockGate.onForeground()
                    else -> Unit
                }
            },
        )
    }
}
