# StudyFlow API

Node.js + Express REST API behind the StudyFlow Android app. It handles authentication (email/password
and Google SSO), tasks, study sessions, XP/badges, user settings and push notifications, matching the
ERD in the Part 1 Planning & Design document.

**Stack:** Node.js 20 · Express · Sequelize ORM · SQLite (local/CI) or **Azure SQL** (production) ·
JWT · bcrypt · Google ID-token verification · Firebase Admin (push) · node-cron

## Run locally
```bash
npm install
cp .env.example .env      # then set JWT_SECRET (and GOOGLE_CLIENT_ID for Google sign-in)
npm run dev               # http://localhost:3000
```
If you change the data model, delete `studyflow.sqlite` so the tables are recreated.

## Test
```bash
npm test                  # 25 tests: auth, bcrypt storage, tasks, sync idempotency, XP, badges, validation
```

## Configuration (environment variables)
| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Secret used to sign login tokens. **Required in production** — use a long random string. |
| `GOOGLE_CLIENT_ID` | The **Web** OAuth client ID (the same one the app uses as `google.webClientId`). |
| `DB_DIALECT` | Set to `mssql` to use Azure SQL. Leave unset for local SQLite. |
| `DB_HOST` `DB_NAME` `DB_USER` `DB_PASS` | Azure SQL connection details. |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | Base64 of the Firebase service-account JSON (enables push on Azure). |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Local alternative: path to the JSON file (default `./firebase-service-account.json`). |

## Deploying to Azure (summary)
1. **Azure SQL Database** (use the free offer if available) → allow Azure services through the server firewall.
2. **App Service** (Linux, Node 20) → set the variables above under *Settings → Environment variables*.
3. Deploy this folder (`az webapp up`, the VS Code Azure extension, or GitHub Actions).
4. Open `https://<your-app>.azurewebsites.net/` — it should return `{"status":"StudyFlow API running"}`.

## Endpoints
All endpoints except `/api/auth/*` need `Authorization: Bearer <JWT>`.

| Method & path | Purpose |
|---|---|
| `POST /api/auth/register` | Create an account (`name`, `email`, `password`) → JWT + user |
| `POST /api/auth/login` | Email/password login → JWT + user |
| `POST /api/auth/google` | Google SSO: send the Google `idToken` → JWT + user |
| `GET /api/tasks` | List the user's tasks (optional `?since=<ISO date>`) |
| `POST /api/tasks` | Create **or update** a task (upsert on the phone-generated `taskId`) |
| `PUT /api/tasks/:id` · `DELETE /api/tasks/:id` | Partial update · delete |
| `GET /api/studysessions` · `POST /api/studysessions` | List · log a session (idempotent on `sessionId`); completed sessions award XP and badges |
| `GET /api/badges` | Badges earned |
| `GET /api/users/me` · `PUT /api/users/me` | Profile (incl. `xpPoints`) · update name / language / theme |
| `DELETE /api/users/me` | Permanently delete the account and all its data |
| `POST /api/notifications/register` · `/unregister` | Register / remove this phone's FCM token |
| `POST /api/notifications/send-test` | Push a test notification to the user's devices |

## What is stored where (for the demo)
`Users` (name, email, **bcrypt `passwordHash`**, language, theme, XP) · `Tasks` · `StudySessions` ·
`Badges` · `DeviceTokens` (Azure SQL). Plain-text passwords are never stored or returned.
