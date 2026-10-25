# JARVIS AI (Phase 2: base app)

Working: navigation (Home/Settings), dark cyan theme, persistent assistant settings (DataStore).
Not yet built: Gemini, voice, wake word, actions, memory, coding, image gen, 3D avatar.

## Build (recommended: GitHub Actions)
1. In Termux: `git init && git add . && git commit -m "phase 2"`
2. Create an empty GitHub repo, then `git remote add origin <url> && git push -u origin main`
3. GitHub -> Actions -> "Build APK" -> download artifact `jarvis-debug-apk`, unzip, install the .apk.

## Build locally (Termux, not guaranteed)
Needs JDK 17, Android SDK (platform 34, build-tools) and Gradle 8.7+. On ARM, Google's aapt2 often fails;
use the GitHub build if so. Command: `gradle assembleDebug`
Output: app/build/outputs/apk/debug/app-debug.apk

## Changing the installed app name
Edit `app_name` in app/src/main/res/values/strings.xml and rebuild.
