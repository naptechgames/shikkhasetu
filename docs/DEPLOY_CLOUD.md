# Cloud deployment (so the app works from any network)

```
Android app ──https──▶ Render web service (Docker: Spring Boot) ──▶ Supabase PostgreSQL
                        built from github.com/naptechgames/shikkhasetu    project "shikkhasetu", schema "shikkhasetu"
```

* The same backend code runs locally (H2 file database) and in the cloud (PostgreSQL). Only
  environment variables differ; they are set on Render, never stored in the repository.
* The tables are in the private schema `shikkhasetu`, not in `public`, so Supabase's own public
  Data API cannot reach them. Only the backend talks to the database.
* `render.yaml` (repository root) describes the service; `backend/Dockerfile` builds it.

**Status: prepared, NOT yet deployed or verified.** Done so far: Supabase project + schema, private
GitHub repository, Render blueprint. The backend has never been run against PostgreSQL, and the
Docker build has never been run (no Docker on the development PC).

(Hugging Face Spaces was tried first; Docker Spaces are no longer available on its free plan.)

## Step 1 — Supabase database password

1. Open <https://supabase.com/dashboard/project/bjkhwqpiukxniscbvkvk/database/settings>.
2. **Reset database password** → choose or generate one → copy it.

## Step 2 — Create the service on Render

1. Open <https://dashboard.render.com/> and sign in with **GitHub** (account `naptechgames`).
   When asked, give Render access to the repository `naptechgames/shikkhasetu`.
2. **New → Blueprint** → pick the repository `shikkhasetu`.
3. Render reads `render.yaml` and asks for five values:

| Name | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&currentSchema=shikkhasetu` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.bjkhwqpiukxniscbvkvk` |
| `SPRING_DATASOURCE_PASSWORD` | the password from step 1 |
| `APP_SEED_COORDINATOR_EMAIL` | the e-mail the coordinator will log in with |
| `APP_SEED_COORDINATOR_PASSWORD` | a new password for the coordinator (at least 6 characters) |

4. **Apply**. The first build takes about 5–10 minutes. The service address is shown at the top,
   normally `https://shikkhasetu.onrender.com` (Render adds a suffix if the name is taken).

## Step 3 — Check, then put the address into the app

```powershell
Invoke-RestMethod https://shikkhasetu.onrender.com/api/health     # {"status":"ok",...}
```

The app's default server address is the constant `defaultBaseUrl` in `mobile/lib/app_state.dart`.
If Render gave a different address, change it there and rebuild the APK
(`cd mobile; flutter build apk --release`). The address can also be typed on the login screen.

## Updating the backend later

`git push` to the `main` branch — Render rebuilds and redeploys automatically.

## Limits of the free tier

* The service **sleeps after 15 minutes without traffic**; the next request takes about a minute
  (the app waits up to 60 s). Open the app a few minutes before a demonstration.
* A free Supabase project is **paused after about a week without activity**; un-pause it in the
  dashboard. Data is kept.
* If the first coordinator was already created, changing `APP_SEED_COORDINATOR_*` later has no
  effect (the seeder runs only when no coordinator exists).
