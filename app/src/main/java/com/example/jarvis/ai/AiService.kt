package com.example.jarvis.ai

/** One message of the conversation. role is "user" or "model". */
data class Turn(val role: String, val text: String)

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String) : AiResult
}

/** The app talks to this interface only, so the AI provider can be swapped later. */
interface AiService {
    suspend fun chat(
        apiKey: String,
        model: String,
        systemPrompt: String,
        turns: List<Turn>
    ): AiResult
}
