package com.example.jarvis.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val settings: StateFlow<AssistantSettings> = repo.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AssistantSettings()
    )

    fun save(s: AssistantSettings) {
        viewModelScope.launch { repo.save(s) }
    }
}
