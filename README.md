# JARVIS AI (Phase 3)

Working: navigation, dark theme, assistant settings, Gemini chat with conversation context,
encrypted API key storage (Android Keystore), safe action router (open https link, YouTube search,
open Settings pages, open dialer). Not yet built: voice, wake word, alarms/timers, memory, coding, image gen, 3D avatar.

## How actions stay safe
Gemini returns JSON -> ReplyParser -> ActionValidator (allowlist + strict checks) -> confirmation
dialog for links and dialer -> ActionExecutor (Android intents). No shell commands, no silent calls.

## Update your repo from Termux
See the phase instructions; then `git add . && git commit -m "phase 3" && git push` and download the APK from GitHub Actions.

## Gemini model
Default `gemini-3.8-flash` (editable in Settings). Check ai.google.dev/gemini-api/docs/changelog for newer names.
