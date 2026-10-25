package com.example.jarvis.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    private val secure = SecureStore(app)

    val settings: StateFlow<AssistantSettings> = repo.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AssistantSettings()
    )

    private val _hasKey = MutableStateFlow(secure.hasApiKey())
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    fun save(s: AssistantSettings) {
        viewModelScope.launch { repo.save(s) }
    }

    fun saveKey(key: String) {
        secure.putApiKey(key)
        _hasKey.value = secure.hasApiKey()
    }

    fun deleteKey() {
        secure.clearApiKey()
        _hasKey.value = false
    }
}
