package com.example.jarvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class GeminiService : AiService {

    override suspend fun chat(
        apiKey: String,
        model: String,
        systemPrompt: String,
        turns: List<Turn>
    ): AiResult = withContext(Dispatchers.IO) { call(apiKey, model, systemPrompt, turns) }

    private fun call(apiKey: String, model: String, systemPrompt: String, turns: List<Turn>): AiResult {
        if (!MODEL_REGEX.matches(model)) {
            return AiResult.Failure("Invalid model name. Use letters, digits, dots and dashes only.")
        }
        val contents = JSONArray()
        turns.forEach { t ->
            contents.put(
                JSONObject()
                    .put("role", t.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", t.text)))
            )
        }
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) AiResult.Failure(httpMessage(code, text)) else extract(text)
        } catch (e: SocketTimeoutException) {
            AiResult.Failure("The request timed out. Check your connection and try again.")
        } catch (e: UnknownHostException) {
            AiResult.Failure("No internet connection.")
        } catch (e: IOException) {
            AiResult.Failure("Network error: ${e.message ?: "unknown"}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun extract(raw: String): AiResult {
        return try {
            val parts = JSONObject(raw)
                .optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
            val sb = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) sb.append(parts.optJSONObject(i)?.optString("text").orEmpty())
            }
            if (sb.isBlank()) {
                AiResult.Failure("Empty response (it may have been blocked by safety filters). Try rephrasing.")
            } else {
                AiResult.Success(sb.toString())
            }
        } catch (e: JSONException) {
            AiResult.Failure("Could not read the server response.")
        }
    }

    private fun httpMessage(code: Int, body: String): String {
        val detail = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty().take(160)
        } catch (e: JSONException) {
            ""
        }
        val base = when (code) {
            400 -> "Bad request (check the model name and API key)."
            401, 403 -> "API key rejected or has no access. Check the key in Settings."
            404 -> "Model not found. Change the model name in Settings."
            429 -> "Rate limit reached. Wait a little and try again."
            in 500..599 -> "Gemini server is busy. Try again shortly."
            else -> "Request failed (HTTP $code)."
        }
        return if (detail.isNotBlank()) "$base\n$detail" else base
    }

    private companion object {
        val MODEL_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")
    }
}
