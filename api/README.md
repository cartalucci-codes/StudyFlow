# StudyFlow API

Node.js + Express REST API backing the StudyFlow Android app (OPSC6312 Portfolio of Evidence).
Implements auth (email/password + Google SSO), tasks, study sessions, user settings and
push-notification device registration, matching the ERD in the Part 1 Planning and Design document.

## Stack
- Node.js + Express
- Sequelize ORM — SQLite locally / in CI, Azure SQL in production (set `DB_DIALECT=mssql`)
- JWT auth, bcrypt password hashing, Google ID token verification for SSO

## Run locally
```bash
npm install
cp .env.example .env    # then edit JWT_SECRET etc.
npm run dev
```
API will be available at `http://localhost:3000`.

## Test
```bash
npm test
```

## Deploy to Azure App Service (matches the Planning & Design doc)
1. Create an Azure SQL Database and an App Service (Node 20 runtime).
2. In the App Service's Configuration, set the same variables as `.env.example`
   (with `DB_DIALECT=mssql` and your Azure SQL credentials).
3. Deploy with the Azure CLI or the VS Code Azure extension:
   ```bash
   az webapp up --name studyflow-api --resource-group studyflow-rg --runtime "NODE:20-lts"
   ```
4. Update `BASE_URL` in the Android app's `app/build.gradle.kts` to your App Service URL.

## Enabling push notifications (optional)

The API works fine without this — it just skips sending pushes. To turn it on:

1. In the [Firebase Console](https://console.firebase.google.com), create a project (or use the
   same Google Cloud project you used for SSO — Firebase can attach to it).
2. **Project settings (gear icon) → Service accounts → Generate new private key**. This downloads
   a JSON file.
3. Save it as `firebase-service-account.json` in this `api/` folder (already gitignored — never commit it).
4. Restart the API (`npm run dev`). You should see `Firebase Admin initialised - push notifications enabled.`
5. Test it immediately (rather than waiting for the 15-minute scheduled job) by calling, once
   logged in and with a device token registered:
   ```
   POST /api/notifications/send-test
   Authorization: Bearer <your JWT>
   ```
   This is the easiest way to demo real-time notifications on camera — the push arrives within seconds.

## Endpoints
See the endpoint table in the Part 1 Planning and Design document, Section 5 — this implementation matches it exactly.
