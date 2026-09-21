# StudyFlow — Android Study & Task Planner

**OPSC6312 — Portfolio of Evidence, Part 2 (Build)**

StudyFlow is a study and task planner for students, combining assignment/task tracking with a
Pomodoro-style study-session timer and light gamification. This repository contains the working
Kotlin prototype for Part 2, built directly against the specification in the Part 1 Planning and
Design document.

## Purpose

StudyFlow's goal is to bring "plan what to do" and "actually do focused study work" into one app,
learning from the strengths and gaps identified in the Part 1 Research Report (TickTick, Todoist,
Microsoft To Do). See that report for the full comparison.

## Design considerations

- **Offline-first**: every task and study session is written to a local Room database first, so
  the app is fully usable with no connection. A `WorkManager` job (`SyncWorker`) pushes pending
  changes to the API once connectivity returns.
- **Security**: passwords are sent over HTTPS and hashed with bcrypt server-side before storage
  (see `studyflow-api/routes/auth.js`) — never stored or transmitted in plain text.
- **REST API**: a self-built Node.js + Express API (in this repo's `api/` subfolder) exposes
  auth, tasks, study sessions, settings and push-notification device registration, matching
  the ERD in the Planning and Design document. See `api/README.md` for running/deploying it.
- **Multi-language**: full interface localisation for English, Afrikaans and isiZulu via Android
  string resources (`res/values`, `res/values-af`, `res/values-zu`).
- **Gamification**: XP is awarded for completed study sessions (see `/api/studysessions` in the
  backend), encouraging real study behaviour rather than box-ticking.

## Architecture

```
UI (Activities)  →  Repository  →  Room (local cache, offline-first)
                                 →  Retrofit (StudyFlow REST API)
```

- `data/local` — Room entities, DAOs, database
- `data/remote` — Retrofit `ApiService`, DTOs, FCM messaging service
- `data/repository` — `AuthRepository`, `TaskRepository`, `SyncWorker`
- `ui/` — one package per screen (login, register, home, settings, tasks, timer)
- `util/` — `PasswordHasher` (bcrypt), `SessionManager` (JWT + prefs)

## Features implemented in this prototype

- [x] Register / log in, password never stored or sent in plain text
- [x] Change settings (language, theme, notifications, log out)
- [x] Connect to a self-built REST API (`studyflow-api/`)
- [x] Offline-first task storage with background sync (Room + WorkManager)
- [x] Study/Pomodoro timer that logs sessions and awards XP
- [x] Multi-language UI: English, Afrikaans, isiZulu
- [x] Unit tests (`app/src/test`)
- [ ] Google SSO — button is wired up; plug in your own Google OAuth client ID
      (see `ui/login/LoginActivity.kt`) to finish this for the final PoE
- [ ] Push notifications — FCM service is scaffolded (`StudyFlowMessagingService`); requires your
      own `google-services.json` (not committed — see `.gitignore`)

## Setting up Google SSO

1. In the [Google Cloud Console](https://console.cloud.google.com), create a project (or reuse one) and go to **APIs & Services → Credentials**.
2. Configure the **OAuth consent screen** first if prompted (External, fill in app name + your email, no scopes needed beyond the defaults).
3. Create **two** OAuth 2.0 Client IDs:
   - **Web application** type — no redirect URIs needed. This one's Client ID goes in two places: `app/src/main/res/values/strings.xml` (`default_web_client_id`) and the backend's `.env` (`GOOGLE_CLIENT_ID`).
   - **Android** type — package name `com.studyflow.app`, plus your debug keystore's SHA-1 fingerprint. Get it with:
     ```powershell
     cd StudyFlow
     .\gradlew signingReport
     ```
     Look for the `SHA1` line under the `debug` variant and paste it in.
4. Rebuild and run — the Google button now launches a real sign-in flow.

## Setting up push notifications

1. In the [Firebase Console](https://console.firebase.google.com), create/select a project (can be the same Google Cloud project used for SSO above) and add an Android app to it with package name `com.studyflow.app`.
2. Download the generated **`google-services.json`** and place it directly inside the `app/` folder (next to `build.gradle.kts`) — it's gitignored, so it won't be committed.
3. On the backend side, follow the "Enabling push notifications" steps in `api/README.md` to generate a service account key — this is what lets the *server* send pushes; the `google-services.json` above is what lets the *app* receive them.
4. Rebuild the app. On first launch it'll ask for notification permission (Android 13+) and silently register the device's token with the API.
5. To test end-to-end without waiting for the scheduled reminder job, call `POST /api/notifications/send-test` (see `api/README.md`) while the app is running — a real push should land within seconds.

## Running this project

1. Open in Android Studio (Koala or newer). It will generate the Gradle wrapper JAR automatically
   on first sync — the wrapper config is already in `gradle/wrapper/gradle-wrapper.properties`.
2. Set `BASE_URL` in `app/build.gradle.kts` to your hosted `studyflow-api` URL (or
   `http://10.0.2.2:3000/` to hit a locally-running API from the Android emulator).
3. Run the backend from the `api/` subfolder before testing network features:
   ```bash
   cd api
   npm install
   cp .env.example .env   # then edit JWT_SECRET etc.
   npm run dev
   ```
4. Build & run on a device/emulator (minSdk 24).

## Testing

Unit tests live in `app/src/test`. Run them with:
```bash
./gradlew testDebugUnitTest
```
GitHub Actions runs this automatically on every push/PR — see `.github/workflows/build.yml`.

## Video demonstration

📹 [Add your unlisted YouTube link here once recorded]

## AI tool usage disclosure

> Fill this section in yourself, honestly and specifically — this is a graded requirement
> (max 500 words). Note where you used AI (e.g. Claude) for scaffolding the Room/Retrofit
> architecture, generating boilerplate screens, or debugging errors, and what you changed,
> tested, or wrote yourself. Include a couple of concrete examples (a prompt/snippet + what you
> did with the output) rather than a vague statement.

## References

See the Part 1 Research Report and Planning and Design document for full references.
