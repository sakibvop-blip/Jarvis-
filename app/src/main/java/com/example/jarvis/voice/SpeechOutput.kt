package com.example.jarvis.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.example.jarvis.settings.AssistantSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceInfo(val name: String, val tag: String, val needsNetwork: Boolean)

/** Text-to-speech using the voices installed on this phone (Android TextToSpeech). */
class SpeechOutput(context: Context) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val voices: StateFlow<List<VoiceInfo>> = _voices.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { code ->
            if (code == TextToSpeech.SUCCESS) {
                _ready.value = true
                _voices.value = loadVoices()
            } else {
                _status.value = "The text-to-speech engine could not start. Check Settings > Text-to-speech output."
            }
        }
    }

    fun refreshVoices() {
        _voices.value = loadVoices()
    }

    private fun loadVoices(): List<VoiceInfo> = try {
        tts?.voices.orEmpty()
            .filter { it.locale.language == "bn" || it.locale.language == "en" }
            .map { VoiceInfo(it.name, it.locale.toLanguageTag(), it.isNetworkConnectionRequired) }
            .sortedWith(compareBy({ it.tag }, { it.name }))
    } catch (e: Exception) {
        emptyList()
    }

    /** Speaks the text. Returns an error message, or null when speech started. */
    fun speak(text: String, s: AssistantSettings): String? {
        val engine = tts
        if (engine == null || !_ready.value) {
            return report("The voice engine is not ready yet. Try again in a moment.")
        }
        val clean = text.trim().take(3000)
        if (clean.isEmpty()) return null

        val isBangla = clean.any { it in '\u0980'..'\u09FF' }
        val wanted = if (isBangla) s.voiceBn else s.voiceEn

        var applied = false
        if (wanted.isNotEmpty()) {
            val voice = try {
                engine.voices?.firstOrNull { it.name == wanted }
            } catch (e: Exception) {
                null
            }
            if (voice != null && engine.setVoice(voice) == TextToSpeech.SUCCESS) applied = true
        }
        if (!applied) {
            val locale = Locale.forLanguageTag(if (isBangla) "bn-BD" else "en-US")
            val r = engine.setLanguage(locale)
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                return report(
                    if (isBangla) "No Bangla voice is installed. Install one in Settings > Text-to-speech output."
                    else "No English voice is installed. Install one in Settings > Text-to-speech output."
                )
            }
        }
        engine.setSpeechRate(s.ttsRate)
        engine.setPitch(s.ttsPitch)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, s.ttsVolume.coerceIn(0f, 1f))
        }
        val result = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "jarvis-utterance")
        if (result != TextToSpeech.SUCCESS) return report("The voice engine refused to speak.")
        _status.value = null
        return null
    }

    private fun report(message: String): String {
        _status.value = message
        return message
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _ready.value = false
    }
}
