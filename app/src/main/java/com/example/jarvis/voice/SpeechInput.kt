package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tap-to-talk speech recognition. Listens only after the user taps the mic,
 * and stops by itself after one phrase. Must be used from the main thread.
 */
class SpeechInput(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    fun start(languageTag: String, onHeard: (String) -> Unit, onFailed: (String) -> Unit) {
        if (_listening.value) return
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onFailed("Speech recognition is not available on this phone. Install or enable the Google app.")
            return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (t.isNotEmpty()) _partial.value = t
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                finish()
                if (text.isEmpty()) onFailed("I did not catch that. Please try again.") else onHeard(text)
            }

            override fun onError(error: Int) {
                finish()
                onFailed(describe(error))
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        _partial.value = ""
        _listening.value = true
        r.startListening(intent)
    }

    /** Stop listening and deliver what was heard so far. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Abort without any result. */
    fun cancel() {
        recognizer?.cancel()
        finish()
    }

    fun destroy() {
        finish()
    }

    private fun finish() {
        _listening.value = false
        _partial.value = ""
        recognizer?.destroy()
        recognizer = null
    }

    private fun describe(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I did not catch that. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I heard nothing. Tap the mic and speak."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech recognition needs internet (or an offline language pack). Check your connection."
        SpeechRecognizer.ERROR_AUDIO -> "Microphone problem. Close other apps that use the mic."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The recognizer is busy. Try again in a moment."
        SpeechRecognizer.ERROR_SERVER -> "The speech service had a problem. Try again."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted."
        else -> "Speech recognition failed (code $code). The language may not be supported on this phone."
    }
}
