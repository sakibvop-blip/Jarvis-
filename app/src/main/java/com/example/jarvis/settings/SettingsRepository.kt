package com.example.jarvis.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

data class AssistantSettings(
    val assistantName: String = "Jarvis",
    val nickname: String = "Boss",
    val greeting: String = "Hello, how can I help you?",
    val displayName: String = "JARVIS AI",
    val personality: String = "Friendly, concise and helpful",
    val language: String = "en", // "en" or "bn"
    val geminiModel: String = "gemini-3.8-flash"
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val name = stringPreferencesKey("assistant_name")
        val nickname = stringPreferencesKey("nickname")
        val greeting = stringPreferencesKey("greeting")
        val displayName = stringPreferencesKey("display_name")
        val personality = stringPreferencesKey("personality")
        val language = stringPreferencesKey("language")
        val model = stringPreferencesKey("gemini_model")
    }

    val settings: Flow<AssistantSettings> = context.dataStore.data.map { p: Preferences ->
        val d = AssistantSettings()
        AssistantSettings(
            assistantName = p[Keys.name] ?: d.assistantName,
            nickname = p[Keys.nickname] ?: d.nickname,
            greeting = p[Keys.greeting] ?: d.greeting,
            displayName = p[Keys.displayName] ?: d.displayName,
            personality = p[Keys.personality] ?: d.personality,
            language = p[Keys.language] ?: d.language,
            geminiModel = p[Keys.model] ?: d.geminiModel
        )
    }

    suspend fun save(s: AssistantSettings) {
        context.dataStore.edit {
            it[Keys.name] = s.assistantName.trim().ifEmpty { "Jarvis" }
            it[Keys.nickname] = s.nickname.trim()
            it[Keys.greeting] = s.greeting.trim()
            it[Keys.displayName] = s.displayName.trim().ifEmpty { "JARVIS AI" }
            it[Keys.personality] = s.personality.trim()
            it[Keys.language] = s.language
            it[Keys.model] = s.geminiModel.trim().ifEmpty { AssistantSettings().geminiModel }
        }
    }
}
