package com.example.jarvis.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.json.JSONObject

/** The only things the assistant is able to do. Anything else is rejected. */
sealed interface Action {
    data class OpenUrl(val url: String) : Action
    data class YoutubeSearch(val query: String) : Action
    data class OpenSettings(val page: String) : Action
    data class Dial(val number: String) : Action
}

sealed interface ActionDecision {
    object None : ActionDecision
    data class Run(val action: Action, val needsConfirm: Boolean, val description: String) : ActionDecision
    data class Rejected(val reason: String) : ActionDecision
}

object ActionValidator {
    private val SETTINGS_PAGES = setOf("main", "wifi", "bluetooth", "sound", "display")
    private val PHONE_REGEX = Regex("^\\+?[0-9]{3,15}$")

    fun validate(json: JSONObject?): ActionDecision {
        if (json == null) return ActionDecision.None
        return when (json.optString("type", "none").trim().lowercase()) {
            "", "none" -> ActionDecision.None

            "open_url" -> {
                val raw = json.optString("url").trim()
                val uri = runCatching { Uri.parse(raw) }.getOrNull()
                val ok = uri != null && uri.scheme?.lowercase() == "https" &&
                    !uri.host.isNullOrBlank() && raw.length <= 500
                if (ok) ActionDecision.Run(Action.OpenUrl(raw), true, "Open this website?\n${uri!!.host}")
                else ActionDecision.Rejected("Only valid https links are allowed.")
            }

            "youtube_search" -> {
                val q = json.optString("query").trim()
                if (q.isEmpty() || q.length > 200) ActionDecision.Rejected("Invalid YouTube search.")
                else ActionDecision.Run(Action.YoutubeSearch(q), false, "Search YouTube for: $q")
            }

            "open_settings" -> {
                val page = json.optString("page", "main").trim().lowercase().ifEmpty { "main" }
                if (page in SETTINGS_PAGES) ActionDecision.Run(Action.OpenSettings(page), false, "Open $page settings")
                else ActionDecision.Rejected("Unknown settings page.")
            }

            "dial" -> {
                val number = json.optString("number").replace(Regex("[\\s\\-()]"), "")
                if (PHONE_REGEX.matches(number)) {
                    ActionDecision.Run(Action.Dial(number), true, "Open the dialer with $number?\n(You still have to press the call button.)")
                } else {
                    ActionDecision.Rejected("That phone number is not valid.")
                }
            }

            else -> ActionDecision.Rejected("This action is not allowed.")
        }
    }
}

class ActionExecutor(private val context: Context) {
    fun execute(action: Action): Result<Unit> {
        val intent = when (action) {
            is Action.OpenUrl -> Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
            is Action.YoutubeSearch -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(action.query))
            )
            is Action.OpenSettings -> Intent(
                when (action.page) {
                    "wifi" -> Settings.ACTION_WIFI_SETTINGS
                    "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                    "sound" -> Settings.ACTION_SOUND_SETTINGS
                    "display" -> Settings.ACTION_DISPLAY_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
            )
            is Action.Dial -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", action.number, null))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }
}
