package com.example.jarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatScreen(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showRationale by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.toggleMic() else vm.micDenied()
    }

    fun onMicClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted || state.listening) vm.toggleMic() else showRationale = true
    }

    LaunchedEffect(state.messages.size, state.sending) {
        val count = state.messages.size + if (state.sending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chat", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = vm::clearChat, enabled = state.messages.isNotEmpty() || state.listening) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear chat")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Text(
                        "Type or tap the mic. Try: \"open YouTube and search lo-fi music\" or \"WiFi settings kholo\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.messages) { Bubble(it) }
            if (state.sending) {
                item { Text("Thinking...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        if (state.listening) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "\u25CF Microphone is ON. Listening... (tap \u23F9 to stop)",
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.labelLarge
                )
                if (state.partial.isNotEmpty()) {
                    Text(state.partial, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                maxLines = 4
            )
            IconButton(
                onClick = { onMicClick() },
                enabled = !state.sending,
                modifier = Modifier.semantics {
                    contentDescription = if (state.listening) "Stop listening" else "Start voice input"
                }
            ) {
                Text(if (state.listening) "\u23F9" else "\uD83C\uDFA4", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(
                onClick = { vm.send(input); input = "" },
                enabled = input.isNotBlank() && !state.sending
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    state.pending?.let { p ->
        AlertDialog(
            onDismissRequest = vm::dismissPending,
            title = { Text("Confirm action") },
            text = { Text(p.description) },
            confirmButton = { TextButton(onClick = vm::confirmPending) { Text("Yes") } },
            dismissButton = { TextButton(onClick = vm::dismissPending) { Text("Cancel") } }
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow microphone?") },
            text = {
                Text(
                    "The mic is used only after you tap the mic button, for one phrase. " +
                        "While it is on, a red \"Microphone is ON\" line is shown. " +
                        "Nothing is recorded or saved by this app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Not now") } }
        )
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val isUser = m.kind == Kind.USER
    val container = when (m.kind) {
        Kind.USER -> Color(0xFF0B3A52)
        Kind.ERROR -> Color(0xFF3A1620)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (m.kind) {
        Kind.ERROR -> Color(0xFFFFB4AB)
        Kind.INFO -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = container),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(m.text, color = textColor, modifier = Modifier.padding(12.dp))
        }
    }
}
