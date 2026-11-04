# StudyFlow release notes

## v2.0.0 — Final PoE (current)

Everything that changed since the **v1.0.0 prototype** (Part 2). Items marked ⭐ are the two
**innovative features** from the Part 1 Planning & Design document, now fully implemented.

### ⭐ Innovative feature 1 — Study-session gamification
XP, levels, streaks and badges are earned for *real study behaviour*, not just ticking boxes.
- **XP:** 1 XP per focused minute in a *completed* study session, plus 10 XP per completed task.
- **Levels:** a new level every 100 XP, shown as a progress ring on the Home screen.
- **Streaks:** consecutive days with at least one completed focus session (🔥 on Home). A streak
  survives until a whole day is missed.
- **Badges:** First session, 3-day, 7-day and 30-day streak badges.
- Calculated on the phone (so it works offline) *and* on the API (stored in the database), using the
  same rules, with unit tests on both sides.

### ⭐ Innovative feature 2 — South African multi-language support
- Full interface in **English, isiZulu and Afrikaans** (every screen, message, dialog and notification).
- Switch language in **Settings** and the whole app changes immediately (per-app locale API).
- The phone's language is detected on first launch; Android 13+ also lists the languages under the
  app's system settings (`locales_config.xml`).

### New features required for the final PoE
| Feature | What was added |
|---|---|
| **Single sign-on** | "Sign in with Google": the Google ID token is verified server-side by the API and exchanged for a StudyFlow JWT; accounts are created on first sign-in. |
| **Offline mode with sync** | Everything is saved to Room first. A WorkManager job syncs automatically the moment the phone reconnects (even if the app was closed). Offline edits, completions *and deletes* are supported. The server accepts phone-generated IDs, so re-sending never creates duplicates. Two-way sync (pushes local changes, pulls server changes). "You're offline" banner, per-task ⏳/☁ sync indicator, sync status and "Sync now" in Settings. |
| **Real-time notifications** | Firebase Cloud Messaging push. The API sends task-due-soon and deadline-missed alerts on a 15-minute schedule; Settings has a "Send me a test notification" button. A **daily study reminder** at a time the student chooses (works offline). Notification permission is requested on Android 13+. Deadline alerts and the daily reminder can be switched off. |
| **Multi-language** | See innovative feature 2. |
| **Settings** | Display name, avatar, language, light/dark theme (applied immediately), notification preferences, reminder time, sync status, log out, **delete account**. |
| **App icon & image assets** | New adaptive icon (graduation cap + checkmark, round variant), in-app logo, notification icon, Play Store icon (512×512) and feature graphic (1024×500). |

### Part 1 features completed since the prototype
- **Calendar / Tasks screen:** month calendar to pick a day, the tasks due that day, "Show all", and an add button.
- **Add / Edit Task:** edit existing tasks, due date *and time*, priority (Low / Medium / High), subject, notes, reminder, delete with confirmation.
- **Home ("My Day"):** today's and overdue tasks with check-off, XP ring, level, streak, badges, sync status.
- **Study timer:** link a session to a task, choose a length (1 / 15 / 25 / 50 min), progress ring, keeps the screen on, survives rotation, asks before abandoning a session.
- **Task completion** awards XP and syncs.

### User experience and robustness
- Inline validation on every form (email format, password strength, matching passwords, required title) with translated messages — invalid input never crashes the app.
- Friendly, translated messages for no connection / wrong password / email already used / server problems.
- Edge-to-edge layout for Android 15/16, dark mode, Material components, loading indicators.
- Network timeouts tuned for a free-tier server that may need ~30 s to wake up.

### API and database
- Task and study-session endpoints are idempotent upserts keyed by the phone-generated UUID.
- New endpoints: `GET /api/badges`, `DELETE /api/users/me`, `POST /api/notifications/unregister`, `POST /api/notifications/send-test`.
- Input validation on every endpoint and a central error handler (a bad request can no longer crash the server).
- Passwords are bcrypt-hashed and never returned by the API; email addresses are normalised to lower case.
- Device push tokens belong to one account at a time and are removed on log out.
- Azure SQL support (`tedious`) and database connection retry for the serverless tier.

### Publishing readiness
- Targets **API 36 / Android 16** (Google Play's requirement for new apps since 31 Aug 2026), `compileSdk` 36.
- Release signing configuration (keystore kept out of git), version code/name 2 / 2.0.0, in-app account deletion, privacy policy, backup disabled, Play listing graphics.

### Testing and automation
- 24 Android unit tests (gamification rules, validation, API↔database mapping, error handling, password hashing, entities) and 25 API tests (auth, bcrypt storage, tasks, offline-sync idempotency, XP, badges, user isolation, validation, account deletion).
- GitHub Actions builds the app and runs both test suites on every push; the final commit is tagged `final-poe`.
- Logging (`Log.d/i/w/e`) added to repositories, sync, notifications and sign-in.

### Bugs fixed from the prototype
- The Register screen title read "Don't have an account? Register".
- Re-syncing an edited task created a duplicate on the server.
- Language and theme choices were saved but never applied.
- Study sessions were never sent to the server (so no XP was awarded).
- The notification permission / token flow, and notifications when the app is closed, now work.
- Hard-coded English messages replaced with translated string resources.

### Lecturer feedback addressed
> *Add a bullet for each piece of feedback your lecturer gave you on Part 1 / Part 2 and what you
> changed in response. Write this yourself — it must reflect the feedback you actually received.*

---

## v1.0.0 — Part 2 prototype
Register/log in with bcrypt-hashed passwords, settings, REST API connection, Room offline storage with a basic sync worker, study timer, English/Afrikaans/isiZulu strings, unit tests and a GitHub Actions workflow.
