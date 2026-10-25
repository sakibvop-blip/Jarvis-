package com.example.jarvis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00252B),
    secondary = Color(0xFF2979FF),
    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFE6F7FF),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE6F7FF),
    surfaceVariant = Color(0xFF16233A),
    onSurfaceVariant = Color(0xFFA9C7D6)
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JarvisColors, content = content)
}
