# Cloud deployment (so the app works from any network)

```
Android app ──https──▶ Hugging Face Space (Docker: Spring Boot jar) ──▶ Supabase PostgreSQL
                        https://naptechgames-shikkhasetu.hf.space        project "shikkhasetu", schema "shikkhasetu"
```

* The same backend code runs locally (H2 file database) and in the cloud (PostgreSQL). Only
  environment variables differ; they are set on the Space, never stored in the code.
* The tables are in the private schema `shikkhasetu`, not in `public`, so Supabase's own public
  Data API cannot reach them. Only the backend talks to the database.
* The APK's default server address is the Space URL. It can still be changed on the login screen.

**Status: prepared, NOT yet deployed or verified.** The Supabase project and schema exist; the
Space still has to be created and given its secrets (steps below).

## Step 1 — Supabase database password (about 2 minutes)

1. Open <https://supabase.com/dashboard/project/bjkhwqpiukxniscbvkvk> (project **shikkhasetu**).
2. *Project Settings → Database → Reset database password*. Choose a password and keep it.
3. Click **Connect** (top bar) → **Session pooler**. Note the **host** (looks like
   `aws-0-ap-south-1.pooler.supabase.com`) and the **user** (`postgres.bjkhwqpiukxniscbvkvk`).
   Use the *session pooler*, not the direct connection: the direct one is IPv6-only.

## Step 2 — Create the Space (about 3 minutes)

1. Open <https://huggingface.co/new-space>, logged in as **naptechgames**.
2. Space name: **`shikkhasetu`** (exactly — the app's default address depends on it).
   SDK: **Docker** → **Blank**. Hardware: free CPU. Visibility: **Public**.
3. In the new Space: *Settings → Variables and secrets* → add:

| Name | Add as | Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Secret | `jdbc:postgresql://<HOST FROM STEP 1>:5432/postgres?sslmode=require&currentSchema=shikkhasetu` |
| `SPRING_DATASOURCE_USERNAME` | Secret | `postgres.bjkhwqpiukxniscbvkvk` |
| `SPRING_DATASOURCE_PASSWORD` | Secret | the password from step 1 |
| `APP_SEED_COORDINATOR_EMAIL` | Secret | the e-mail the coordinator will log in with |
| `APP_SEED_COORDINATOR_PASSWORD` | Secret | a new password for the coordinator (not the demo one) |
| `APP_SEED_DEMO_DATA` | Variable | `false` |

## Step 3 — Upload the three files

Folder: `deploy\huggingface\` (`Dockerfile`, `README.md`, `shikkhasetu-backend-1.0.0.jar`).

* **In the browser:** Space → *Files* → *Contribute → Upload files* → drag the three files →
  *Commit*. (Replace the README that the Space created.)
* **Or from PowerShell** (asks for a Hugging Face *write* token once):

```powershell
hf auth login
hf upload naptechgames/shikkhasetu deploy\huggingface . --repo-type space
```

The Space builds for 1–3 minutes. When it shows **Running**, open
<https://naptechgames-shikkhasetu.hf.space/api/health> → `{"status":"ok",…}`.

## Step 4 — Check

```powershell
Invoke-RestMethod https://naptechgames-shikkhasetu.hf.space/api/health
```

Then install `release\ShikkhaSetu-1.0.0.apk`, register a student, and log in as the coordinator
with the e-mail and password from step 2.

## Updating the backend later

```powershell
. .\tools\env.ps1
cd backend; .\gradlew.bat bootJar
copy build\libs\shikkhasetu-backend-1.0.0.jar ..\deploy\huggingface\
```

and upload the jar again (step 3).

## Limits of the free tier

* The Space **sleeps after about 48 hours without traffic**. The first request then takes around a
  minute; the app says "The server is starting up". Open the app a few minutes before a demo.
* A free Supabase project is **paused after about a week without activity**; un-pause it in the
  dashboard. Data is kept.
* The jar in the Space is public (anyone can download it). It contains no passwords: the
  `app.seed.*` values inside it are the local demo defaults, overridden by the Space secrets.
* If the first coordinator was already created, changing `APP_SEED_COORDINATOR_*` later has no
  effect (the seeder runs only when no coordinator exists).
