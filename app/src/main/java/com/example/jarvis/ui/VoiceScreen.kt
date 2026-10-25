package com.example.jarvis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.settings.AssistantSettings
import com.example.jarvis.voice.SpeechOutput
import com.example.jarvis.voice.VoiceInfo

@Composable
fun VoiceScreen(
    settings: AssistantSettings,
    speech: SpeechOutput,
    onSave: (AssistantSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val ready by speech.ready.collectAsStateWithLifecycle()
    val voices by speech.voices.collectAsStateWithLifecycle()
    val status by speech.status.collectAsStateWithLifecycle()

    LaunchedEffect(ready) { if (ready) speech.refreshVoices() }

    var rate by remember(settings.ttsRate) { mutableFloatStateOf(settings.ttsRate) }
    var pitch by remember(settings.ttsPitch) { mutableFloatStateOf(settings.ttsPitch) }
    var volume by remember(settings.ttsVolume) { mutableFloatStateOf(settings.ttsVolume) }

    fun live() = settings.copy(ttsRate = rate, ttsPitch = pitch, ttsVolume = volume)

    val bnVoices = voices.filter { it.tag.startsWith("bn") }
    val enVoices = voices.filter { it.tag.startsWith("en") }
    val name = settings.assistantName

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Voice settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("Speak replies aloud")
            Switch(checked = settings.speakReplies, onCheckedChange = { onSave(settings.copy(speakReplies = it)) })
        }

        if (!ready) {
            Text("Voice engine is starting...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text("Speed: ${"%.1f".format(rate)}x", style = MaterialTheme.typography.titleSmall)
        Slider(rate, { rate = it }, valueRange = 0.5f..2.0f,
            onValueChangeFinished = { onSave(settings.copy(ttsRate = rate)) })
        Text("Pitch: ${"%.1f".format(pitch)}", style = MaterialTheme.typography.titleSmall)
        Slider(pitch, { pitch = it }, valueRange = 0.5f..2.0f,
            onValueChangeFinished = { onSave(settings.copy(ttsPitch = pitch)) })
        Text("Volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
        Slider(volume, { volume = it }, valueRange = 0.0f..1.0f,
            onValueChangeFinished = { onSave(settings.copy(ttsVolume = volume)) })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { speech.speak("Hello, I am $name. This is how I sound.", live()) }) {
                Text("Preview English")
            }
            Button(onClick = { speech.speak("\u09B9\u09CD\u09AF\u09BE\u09B2\u09CB, \u0986\u09AE\u09BF $name\u0964 \u0986\u09AE\u09BE\u09B0 \u0995\u09A3\u09CD\u09A0 \u098F\u09AE\u09A8 \u09B6\u09CB\u09A8\u09BE\u09AC\u09C7\u0964", live()) }) {
                Text("Preview \u09AC\u09BE\u0982\u09B2\u09BE")
            }
        }
        OutlinedButton(onClick = { speech.stop() }) { Text("Stop speaking") }

        VoiceGroup(
            title = "English voices",
            emptyHint = "No English voices found.",
            voices = enVoices,
            selected = settings.voiceEn,
            onSelect = { onSave(settings.copy(voiceEn = it)) }
        )
        VoiceGroup(
            title = "Bangla voices",
            emptyHint = "No Bangla voice found. Install one (steps below), then reopen this screen.",
            voices = bnVoices,
            selected = settings.voiceBn,
            onSelect = { onSave(settings.copy(voiceBn = it)) }
        )

        Text(
            "Android does not tell apps whether a voice is male or female. Pick a voice, press Preview, " +
                "and keep the one you like. Voice names that end in \"network\" need internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("How to install more voices", style = MaterialTheme.typography.titleSmall)
        Text(
            "Phone Settings > search \"Text-to-speech output\" > tap the gear next to Speech Recognition " +
                "& Synthesis by Google (or your engine) > Install voice data > choose Bangla (Bangladesh/India) " +
                "or English > download. Then come back here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoiceGroup(
    title: String,
    emptyHint: String,
    voices: List<VoiceInfo>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    VoiceRow("Phone default", selected.isEmpty()) { onSelect("") }
    if (voices.isEmpty()) {
        Text(emptyHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    voices.forEach { v ->
        VoiceRow(
            label = v.name + " (" + v.tag + ")" + if (v.needsNetwork) " online" else "",
            selected = v.name == selected
        ) { onSelect(v.name) }
    }
}

@Composable
private fun VoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
