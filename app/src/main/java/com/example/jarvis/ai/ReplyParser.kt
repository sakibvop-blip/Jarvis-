package com.example.jarvis.ai

import org.json.JSONException
import org.json.JSONObject

data class ParsedReply(val reply: String, val action: JSONObject?)

object ReplyParser {
    fun parse(raw: String): ParsedReply {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val obj = JSONObject(cleaned)
            val reply = obj.optString("reply").ifBlank { "..." }
            ParsedReply(reply, obj.optJSONObject("action"))
        } catch (e: JSONException) {
            // Malformed JSON: show the text, never run any action.
            ParsedReply(cleaned.ifBlank { "..." }, null)
        }
    }
}
