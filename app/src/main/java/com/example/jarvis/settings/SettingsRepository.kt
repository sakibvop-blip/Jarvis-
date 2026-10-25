package com.example.jarvis.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val geminiModel: String = "gemini-3.6-flash",
    val speakReplies: Boolean = true,
    val voiceEn: String = "",     // chosen English TTS voice name ("" = default)
    val voiceBn: String = "",     // chosen Bangla TTS voice name ("" = default)
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVolume: Float = 1.0f
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
        val speak = booleanPreferencesKey("speak_replies")
        val voiceEn = stringPreferencesKey("voice_en")
        val voiceBn = stringPreferencesKey("voice_bn")
        val rate = floatPreferencesKey("tts_rate")
        val pitch = floatPreferencesKey("tts_pitch")
        val volume = floatPreferencesKey("tts_volume")
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
            geminiModel = p[Keys.model] ?: d.geminiModel,
            speakReplies = p[Keys.speak] ?: d.speakReplies,
            voiceEn = p[Keys.voiceEn] ?: d.voiceEn,
            voiceBn = p[Keys.voiceBn] ?: d.voiceBn,
            ttsRate = p[Keys.rate] ?: d.ttsRate,
            ttsPitch = p[Keys.pitch] ?: d.ttsPitch,
            ttsVolume = p[Keys.volume] ?: d.ttsVolume
        )
    }

    suspend fun save(s: AssistantSettings) {
        val d = AssistantSettings()
        context.dataStore.edit {
            it[Keys.name] = s.assistantName.trim().ifEmpty { d.assistantName }
            it[Keys.nickname] = s.nickname.trim()
            it[Keys.greeting] = s.greeting.trim()
            it[Keys.displayName] = s.displayName.trim().ifEmpty { d.displayName }
            it[Keys.personality] = s.personality.trim()
            it[Keys.language] = s.language
            it[Keys.model] = s.geminiModel.trim().ifEmpty { d.geminiModel }
            it[Keys.speak] = s.speakReplies
            it[Keys.voiceEn] = s.voiceEn
            it[Keys.voiceBn] = s.voiceBn
            it[Keys.rate] = s.ttsRate.coerceIn(0.5f, 2.0f)
            it[Keys.pitch] = s.ttsPitch.coerceIn(0.5f, 2.0f)
            it[Keys.volume] = s.ttsVolume.coerceIn(0.0f, 1.0f)
        }
    }
}
