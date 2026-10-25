package com.example.jarvis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.actions.Action
import com.example.jarvis.actions.ActionDecision
import com.example.jarvis.actions.ActionExecutor
import com.example.jarvis.actions.ActionValidator
import com.example.jarvis.ai.AiResult
import com.example.jarvis.ai.AiService
import com.example.jarvis.ai.GeminiService
import com.example.jarvis.ai.ReplyParser
import com.example.jarvis.ai.SystemPrompt
import com.example.jarvis.ai.Turn
import com.example.jarvis.security.SecureStore
import com.example.jarvis.settings.SettingsRepository
import com.example.jarvis.voice.SpeechInput
import com.example.jarvis.voice.SpeechOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Kind { USER, ASSISTANT, INFO, ERROR }

data class ChatMessage(val text: String, val kind: Kind)

data class PendingAction(val action: Action, val description: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val pending: PendingAction? = null,
    val listening: Boolean = false,
    val partial: String = ""
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val secure = SecureStore(app)
    private val ai: AiService = GeminiService()
    private val executor = ActionExecutor(app)
    private val turns = mutableListOf<Turn>()

    val speech = SpeechOutput(app)
    private val input = SpeechInput(app)
    private var voiceWarned = false

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { input.listening.collect { l -> _state.update { it.copy(listening = l) } } }
        viewModelScope.launch { input.partial.collect { p -> _state.update { it.copy(partial = p) } } }
    }

    private fun add(m: ChatMessage) = _state.update { it.copy(messages = it.messages + m) }

    private fun window(): List<Turn> = turns.takeLast(20).dropWhile { it.role != "user" }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || _state.value.sending) return
        speech.stop()
        add(ChatMessage(t, Kind.USER))

        val key = secure.getApiKey()
        if (key == null) {
            add(ChatMessage("No API key yet. Open Settings and save your Gemini API key.", Kind.ERROR))
            return
        }

        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            turns.add(Turn("user", t))
            when (val r = ai.chat(key, settings.geminiModel, SystemPrompt.build(settings), window())) {
                is AiResult.Failure -> {
                    turns.removeAt(turns.lastIndex)
                    add(ChatMessage(r.message, Kind.ERROR))
                }
                is AiResult.Success -> {
                    turns.add(Turn("model", r.text))
                    val parsed = ReplyParser.parse(r.text)
                    add(ChatMessage(parsed.reply, Kind.ASSISTANT))
                    if (settings.speakReplies) {
                        val problem = speech.speak(parsed.reply, settings)
                        if (problem != null && !voiceWarned) {
                            voiceWarned = true
                            add(ChatMessage("Voice reply unavailable: $problem", Kind.INFO))
                        }
                    }
                    handle(ActionValidator.validate(parsed.action))
                }
            }
            _state.update { it.copy(sending = false) }
        }
    }

    /** Tap-to-talk. Call only after the microphone permission is granted. */
    fun toggleMic() {
        if (_state.value.listening) {
            input.stop()
            return
        }
        if (_state.value.sending) return
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            speech.stop()
            input.start(
                languageTag = if (s.language == "bn") "bn-BD" else "en-US",
                onHeard = { heard -> send(heard) },
                onFailed = { msg -> add(ChatMessage(msg, Kind.ERROR)) }
            )
        }
    }

    fun micDenied() {
        add(
            ChatMessage(
                "Microphone permission was denied, so voice input is off. You can still type. " +
                    "To enable it: phone Settings > Apps > JARVIS AI > Permissions > Microphone.",
                Kind.ERROR
            )
        )
    }

    private fun handle(decision: ActionDecision) {
        when (decision) {
            is ActionDecision.None -> Unit
            is ActionDecision.Rejected -> add(ChatMessage("Blocked: ${decision.reason}", Kind.ERROR))
            is ActionDecision.Run ->
                if (decision.needsConfirm) {
                    _state.update { it.copy(pending = PendingAction(decision.action, decision.description)) }
                } else {
                    run(decision.action, decision.description)
                }
        }
    }

    private fun run(action: Action, description: String) {
        executor.execute(action)
            .onSuccess { add(ChatMessage("Done: $description", Kind.INFO)) }
            .onFailure { add(ChatMessage("Could not do that: no app on this phone can handle it.", Kind.ERROR)) }
    }

    fun confirmPending() {
        val p = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }
        run(p.action, p.description)
    }

    fun dismissPending() {
        if (_state.value.pending != null) {
            _state.update { it.copy(pending = null) }
            add(ChatMessage("Cancelled.", Kind.INFO))
        }
    }

    fun clearChat() {
        speech.stop()
        input.cancel()
        turns.clear()
        _state.update { ChatUiState() }
    }

    override fun onCleared() {
        input.destroy()
        speech.shutdown()
        super.onCleared()
    }
}
