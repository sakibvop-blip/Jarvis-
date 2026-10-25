package com.example.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.jarvis.settings.AssistantSettings

@Composable
fun SettingsScreen(
    current: AssistantSettings,
    hasKey: Boolean,
    onSave: (AssistantSettings) -> Unit,
    onSaveKey: (String) -> Unit,
    onDeleteKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(current) { mutableStateOf(current) }
    var saved by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }

    fun edit(block: (AssistantSettings) -> AssistantSettings) {
        draft = block(draft); saved = false
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Assistant settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(draft.assistantName, { v -> edit { it.copy(assistantName = v) } },
            label = { Text("Assistant name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.nickname, { v -> edit { it.copy(nickname = v) } },
            label = { Text("What should it call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.greeting, { v -> edit { it.copy(greeting = v) } },
            label = { Text("Greeting") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.personality, { v -> edit { it.copy(personality = v) } },
            label = { Text("Personality") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.displayName, { v -> edit { it.copy(displayName = v) } },
            label = { Text("In-app display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(
            "The installed app label (under the launcher icon) only changes by editing app_name in " +
                "res/values/strings.xml and rebuilding the APK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Language", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(draft.language == "en", { edit { it.copy(language = "en") } }, label = { Text("English") })
            FilterChip(draft.language == "bn", { edit { it.copy(language = "bn") } }, label = { Text("বাংলা") })
        }

        OutlinedTextField(draft.geminiModel, { v -> edit { it.copy(geminiModel = v) } },
            label = { Text("Gemini model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(
            "Default: gemini-3.6-flash. If a model stops working, enter a newer name from ai.google.dev.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = { onSave(draft); saved = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Save settings")
        }
        if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)

        Text("Gemini API key", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            if (hasKey) "A key is saved (encrypted on this phone)." else "No key saved yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Paste API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSaveKey(keyInput.trim()); keyInput = "" },
                enabled = keyInput.isNotBlank()
            ) { Text("Save key") }
            OutlinedButton(onClick = onDeleteKey, enabled = hasKey) { Text("Delete key") }
        }
        Text(
            "The key stays on this phone and is sent only to Google's Gemini API. " +
                "Never share it or put it in GitHub.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
