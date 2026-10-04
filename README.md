# ShikkhaSetu

A community-based educational resource lending and donation system — BUBT CSE 327 (Software
Engineering) course project.

Students offer and request textbooks and calculators from their Android phone. A club coordinator
reviews listings, approves requests, hands items over and records returns.

| Part | Technology | Folder |
|---|---|---|
| Android app | Flutter 3.47 / Dart | [`mobile/`](mobile/) |
| Backend REST API | Java 17, Spring Boot 3.5, Spring Data JPA | [`backend/`](backend/) |
| Database | Embedded H2 file database (default) or MySQL (profile) | `backend/data/` (created on first start) |
| Ready-made builds | APK and backend jar | [`release/`](release/) |
| Documents | Patterns, testing evidence | [`docs/`](docs/) |
| Proposal | Original (unchanged) and revised v2 for mobile | `ShikkhaSetu_Project_Proposal_CSE327*.docx` |

## Status — what is and is not verified

| Item | Status |
|---|---|
| Backend: 83 JUnit tests (unit, integration, black-box, basis-path) | **Run, all passed** |
| Backend: end-to-end smoke test over real HTTP with the file database | **Run, passed** |
| Flutter: static analysis and 12 tests | **Run, no issues / all passed** |
| Release APK built and signature checked (`apksigner verify`) | **Done** |
| APK installed and used on a real Android phone | **NOT done — not verified** |
| Phone ↔ laptop connection over Wi-Fi | **NOT tested** |
| MySQL profile | **Written, NOT run** (no MySQL server on the development laptop) |
| Cloud deployment (Render + Supabase PostgreSQL), see [`docs/DEPLOY_CLOUD.md`](docs/DEPLOY_CLOUD.md) | **Prepared, NOT yet deployed or verified.** The backend has never been run against PostgreSQL. |
| Real e-mail sending | **Not implemented** — the e-mail channel is a console stub |

The app is a real multi-user client/server system (all data and rules are on the backend), but
until it has been exercised on real phones it must not be described as device-tested.

## Features

1. Student and coordinator login; role-based access enforced by the backend.
2. Catalogue of books / calculators / other items with text search and filters (category, type, status).
3. Students offer items (donation or loan) and request items (borrow for 1–30 days, or receive a donation).
4. Coordinator approval; the same item can never be allocated twice (transaction + row lock + version column).
5. Handover with a single-use pickup code, due date, overdue flag, return with item condition.
6. In-app notifications and an impact dashboard.

Item status: `PENDING_REVIEW → AVAILABLE → RESERVED → ON_LOAN → AVAILABLE` (loan) or `… → RESERVED → DONATED` (donation).

## 1. One-time setup of the tools

No Android Studio and no emulator are needed. On the development laptop the tools are in
`E:\DevTools` (about 11 GB):

| Tool | Location |
|---|---|
| Flutter 3.47.6 (includes Dart) | `E:\DevTools\flutter` |
| Temurin JDK 17.0.20.1 | `E:\DevTools\jdk-17.0.20.1+1` |
| Android SDK (command-line tools, platform-tools, platform 36, build-tools) | `E:\DevTools\android-sdk` |
| Gradle and pub caches | `E:\DevTools\gradle-home`, `E:\DevTools\pub-cache` |

The Windows variables `JAVA_HOME` (→ `C:\Program Files\Java\jdk-17`) and `ANDROID_SDK_ROOT`
(→ `A:\Android Studio SDK`) point to folders that do not exist. They were **left unchanged**.
Instead, load the correct paths in every new PowerShell window:

```powershell
. .\tools\env.ps1
```

(The leading dot and space are required.) Check with `flutter doctor`; "Android toolchain" must be
green. The Visual Studio warning is irrelevant — it is only for Windows desktop apps.

On another computer: install Flutter, a JDK 17 and the Android command-line tools, then edit the
`$DevTools` path at the top of `tools/env.ps1`.

## 2. Run the backend

```powershell
. .\tools\env.ps1
cd backend
.\gradlew.bat bootRun
```

or, without Gradle, the ready-made jar (run it from the `backend` folder so the database is created there):

```powershell
. .\tools\env.ps1
cd backend
java -jar ..\release\shikkhasetu-backend-1.0.0.jar
```

The server listens on port 8080 on all network interfaces. Check: open
<http://localhost:8080/api/health> in a browser → `{"status":"ok", …}`.

On the first start it creates a coordinator account, a demo student and four synthetic demo items.
The e-mail addresses and passwords of these **demo accounts** are in
`backend/config/application.properties` (`app.seed.*`). That file is not in the repository: copy
`backend/config/application.properties.example` to that name and choose passwords. New students register inside the app; coordinators
can only be created through this seed setting.

**Using MySQL instead of H2** (not tested, see status table): create a database `shikkhasetu`, set
the environment variables `MYSQL_USER` / `MYSQL_PASSWORD`, and start with
`java -jar … --spring.profiles.active=mysql`.

**Backup / restore:** stop the server and copy the folder `backend\data`. To restore, stop the
server and copy the folder back. To start with an empty database, delete the folder.

## 3. Install the app on an Android phone

Requirements: Android 7.0 (API 24) or newer; phone and laptop on the **same Wi-Fi network**.

**Option A — copy the APK.** Copy `release\ShikkhaSetu-1.0.0.apk` to the phone (USB, Drive, …), tap
it and allow "install from unknown sources". The APK is signed with the Android *debug* key: fine
for a demonstration, not for the Play Store.

**Option B — install over USB with adb.** On the phone enable *Developer options → USB debugging*,
connect the cable, accept the prompt on the phone, then:

```powershell
. .\tools\env.ps1
adb devices                                   # the phone must be listed as "device"
adb install -r release\ShikkhaSetu-1.0.0.apk
```

**Connect the app to the backend**

The app's default server address is the cloud backend
(`https://shikkhasetu.onrender.com`, see [`docs/DEPLOY_CLOUD.md`](docs/DEPLOY_CLOUD.md)),
which works from any network once it is deployed. To use a backend on the laptop instead:

1. Find the laptop's Wi-Fi address: `ipconfig` → "IPv4 Address", e.g. `192.168.88.249`.
2. In the app's login screen set **Server address** to `http://<that address>:8080`.
3. If the app says "Cannot reach the server": open `http://<that address>:8080/api/health` in the
   phone's browser. If that fails too, Windows Firewall is blocking port 8080 — allow Java / TCP
   port 8080 for *private* networks in "Windows Defender Firewall → Allow an app". (This setting was
   deliberately not changed for you.)

**Develop with hot reload on the phone:** `cd mobile; flutter run`.
**Rebuild the APK:** `cd mobile; flutter build apk --release` → `mobile\build\app\outputs\flutter-apk\app-release.apk`.

## 4. A demonstration script (two phones, or one phone logging in twice)

1. Student registers ("New student? Create an account") and logs in.
2. Student: *Catalogue* → search "casio" → **Request to borrow** → 7 days.
3. Student: **Offer item** → a book as *Donation* → it is "Pending review" and invisible to others.
4. Coordinator logs in: *Catalogue* → Status: *Pending review* → **Approve listing**.
5. Coordinator: *Requests* → **Approve**. The item becomes *Reserved*. A second request for the same
   item can no longer be approved (try it with a second student: "already allocated").
6. Student: *Requests* shows the **pickup code**; *Alerts* shows the notification.
7. Coordinator: **Hand over** → type the code → item is *On loan* with a due date.
8. Coordinator: **Record return** → choose the condition → item is *Available* again.
9. Both: *Impact* shows the updated numbers.

## 5. Tests

```powershell
. .\tools\env.ps1
cd backend; .\gradlew.bat test      # 83 tests; HTML report in build\reports\tests\test\index.html
cd ..\mobile; flutter test           # 12 tests
cd ..; .\tools\smoke-test.ps1        # needs a running backend with a fresh database
```

Details, techniques (equivalence classes, boundary values, control-flow graph, cyclomatic
complexity), results and the defect log: [`docs/TESTING.md`](docs/TESTING.md).

## 6. Design

Design patterns (Factory Method, Strategy, Observer, Adapter) with class tables and viva
explanations: [`docs/DESIGN_PATTERNS.md`](docs/DESIGN_PATTERNS.md).

```
mobile (Flutter)                      backend (Spring Boot)
  screens/  ── api_client.dart ──HTTP/JSON──▶ web/ApiController      (JSON ⇄ services)
  app_state.dart (session)                   web/AuthInterceptor     (token → current user)
  models.dart                                service/*               (ALL rules and permissions)
                                             workflow/ allocation/ event/ notify/   (patterns)
                                             repo/ + model/          (JPA entities, H2 / MySQL)
```

The app contains no business rules: it shows buttons according to the role, but every permission
and allocation rule is checked again by the backend (`service/RequestService.java`,
`service/ItemService.java`). Passwords are stored as BCrypt hashes. A pickup code is sent only to
the student who made the request.

### REST API

| Method and path | Who | Purpose |
|---|---|---|
| `GET /api/health` | anyone | server check |
| `POST /api/auth/register`, `/login`, `/logout` | anyone / logged in | accounts and session token |
| `GET /api/items?q=&category=&mode=&status=` | logged in | search and filter |
| `GET /api/items/mine`, `POST /api/items` | logged in | own listings, offer an item |
| `POST /api/items/{id}/review` | coordinator | accept / reject a listing |
| `POST /api/items/{id}/allocate-fcfs` | coordinator | approve the oldest pending request |
| `POST /api/requests`, `GET /api/requests/mine` | student | request an item, own requests |
| `GET /api/requests`, `GET /api/requests/{id}` | coordinator / owner | all requests, one request |
| `POST /api/requests/{id}/approve`, `/reject` | coordinator | decide |
| `POST /api/requests/{id}/cancel` | owner or coordinator | cancel before handover |
| `POST /api/requests/{id}/handover`, `/return` | coordinator | pickup code check, return |
| `GET /api/notifications`, `POST /api/notifications/{id}/read` | logged in | in-app notifications |
| `GET /api/dashboard` | logged in | impact numbers |

## 7. Known limitations

* Plain HTTP on the local network only; a public deployment needs HTTPS.
* Session tokens do not expire until logout.
* No item photos, no push notifications (the app shows notifications when the *Alerts* tab is opened), no iOS build.
* Overdue loans are flagged, but no automatic reminder is sent.
* Differences from the first proposal are listed in section 0 of
  `ShikkhaSetu_Project_Proposal_CSE327_v2_Mobile.docx` (regenerate with `node generate_proposal_v2_mobile.cjs`).
