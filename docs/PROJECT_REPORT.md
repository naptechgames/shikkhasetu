# ShikkhaSetu — Project Work Report

**Course:** CSE 327 Software Engineering, BUBT
**Project:** ShikkhaSetu — a community-based educational resource lending and donation system
**Date of this report:** 5 October 2026

| | |
|---|---|
| Source code (GitHub) | https://github.com/naptechgames/shikkhasetu |
| Live backend server | https://shikkhasetu.onrender.com (health check: `/api/health`) |
| Android app (APK) | `release/ShikkhaSetu-1.0.0.apk` |

## 1. What the project does

Students of a university club share textbooks and calculators. A student can **offer** an item
(as a donation or as a loan) and can **request** an item. A club **coordinator** reviews offered
items, approves requests, hands the item over and records the return.

The system has two parts:

1. An **Android app** (Flutter) used by students and the coordinator.
2. A **backend server** (Java, Spring Boot) with a database. All data and all rules are on the
   server, so many phones share the same data at the same time.

## 2. Technology used

| Part | Technology | Why |
|---|---|---|
| Mobile app | Flutter 3.47, Dart | One code base, native Android app, simple UI code |
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA | Transactions and row locking for safe allocation; JUnit for testing |
| Database (local) | H2 file database | No separate database server to install for development |
| Database (cloud) | PostgreSQL on Supabase | Free, permanent storage for the live server |
| Hosting | Render (Docker container) | Free hosting, so the app works from any network |
| Testing | JUnit 5, Mockito, Spring MockMvc, flutter_test, JaCoCo | Unit, integration, black-box, white-box tests and coverage |
| Version control | Git, GitHub | History of the work |

The first proposal planned a React web frontend. The requirement changed to a mobile app, so the
frontend was built in Flutter. The revised proposal is in
`ShikkhaSetu_Project_Proposal_CSE327_v2_Mobile.docx`; the original proposal is unchanged.

## 3. Size of the work

| Part | Files | Lines of code |
|---|---|---|
| Backend application code (Java) | 34 | 1,633 |
| Backend test code (Java) | 7 | 848 |
| Mobile app code (Dart) | 12 | 1,296 |
| Mobile test code (Dart) | 3 | 176 |
| **Total** | **56** | **3,953** |

## 4. Features that were built

| # | Feature | Where in the app | Where in the code |
|---|---|---|---|
| 1 | Student registration, login, logout | Login screen | `AuthService.java`, `login_screen.dart` |
| 2 | Two roles (student, coordinator) with different permissions, checked on the server | Whole app | `AuthInterceptor.java`, `RequestService.java`, `ItemService.java` |
| 3 | Catalogue of books / calculators / other items | Catalogue tab | `ItemService.java`, `catalogue_screen.dart` |
| 4 | Search by text and filter by category, type and status | Catalogue tab | `ItemService.search()`, `catalogue_screen.dart` |
| 5 | Offer an item for donation or loan | "Offer item" button | `ItemService.create()`, `offer_item_screen.dart` |
| 6 | Coordinator reviews offered items (approve / reject) | Catalogue → Status: Pending review | `ItemService.review()` |
| 7 | Request to borrow (1–30 days) or to receive a donation | "Request" button on an item | `RequestService.create()` |
| 8 | Coordinator approves or rejects a request; the item becomes Reserved | Requests tab | `RequestService.allocate()`, `reject()` |
| 9 | The same item can never be given to two students | (server rule) | `RequestService.allocate()`, `ItemRepository.findByIdForUpdate()` |
| 10 | Handover with a single-use 6-digit pickup code | Requests tab | `RequestService.handOver()` |
| 11 | Due date, overdue flag, return with item condition | Requests tab | `LoanWorkflow.java`, `RequestService.returnItem()` |
| 12 | Cancel a request before handover (reservation is released) | Requests tab | `RequestService.cancel()` |
| 13 | In-app notifications | Alerts tab | `NotificationListener.java`, `notifications_screen.dart` |
| 14 | Impact dashboard (donations, loans, recipients, overdue) | Impact tab | `DashboardService.java`, `dashboard_screen.dart` |
| 15 | Audit log of every action | (database) | `AuditLogListener.java` |

**Item status flow**

* Loan: Pending review → Available → Reserved → On loan → Available
* Donation: Pending review → Available → Reserved → Donated

## 5. Structure of the code

```
backend/src/main/java/bd/bubt/shikkhasetu/
  model/       database tables as Java classes (User, ResourceItem, ResourceRequest, ...)
  repo/        database access (Spring Data repositories)
  service/     ALL business rules and permission checks
  workflow/    Factory Method pattern  (loan vs donation)
  allocation/  Strategy pattern        (who gets the item)
  event/       Observer pattern        (notifications, audit log)
  notify/      Adapter pattern         (in-app and e-mail channels)
  web/         REST API (JSON), login token check, error messages

mobile/lib/
  main.dart         start of the app
  app_state.dart    login session saved on the phone
  api_client.dart   all calls to the backend
  models.dart       data classes
  screens/          login, catalogue, offer item, requests, notifications, dashboard
```

The mobile app has **no business rules**. It only shows screens and sends requests. Every rule is
checked by the server, so a changed or old app cannot break the rules.

## 6. Design patterns (course requirement)

| Pattern | Problem it solves here | Classes |
|---|---|---|
| **Factory Method** | A loan and a donation follow different rules (loan period, due date, return). The service should not be full of `if loan ... else donation`. | `WorkflowCreator` (creator) with `LoanWorkflowCreator`, `DonationWorkflowCreator`; products `LoanWorkflow`, `DonationWorkflow` implementing `RequestWorkflow` |
| **Strategy** | Several students can request one item. The rule for choosing the winner can change. | `AllocationPolicy` with `FirstComeFirstServed` and `CoordinatorChoice`; used by `RequestService.allocate()` |
| **Observer** | After an approval the student must be notified and the action logged. The service should not know who needs to react. | `EventBus` (subject), `DomainEventListener` (observer), `NotificationListener`, `AuditLogListener` |
| **Adapter** | Our code calls `send(user, message)`; an external e-mail provider has a different method `deliver(address, subject, body)`. | `NotificationChannel` (target), `EmailNotificationAdapter` (adapter), `ExternalEmailClient` (adaptee) |

Honest note: the e-mail provider is a **stub** that prints to the console. Real e-mail is not sent.
Full explanation with a walk-through: `docs/DESIGN_PATTERNS.md`.

## 7. How double allocation is prevented

This is the most important rule of the system. `RequestService.allocate()`:

1. The whole method runs in **one database transaction**.
2. The item row is read with a **lock** (`SELECT ... FOR UPDATE`). A second approval for the same
   item must wait until the first one finishes.
3. After waiting, the second approval sees that the item is already **Reserved** and is refused
   (HTTP 409).
4. Extra safety: the item has a **version column** (optimistic locking).

A test starts two approvals for the same item at the same moment, 10 times. Every time exactly one
succeeds.

## 8. Testing (course requirement)

| Type | Tests | Result | What is tested |
|---|---|---|---|
| Unit | 28 | all passed | allocation policies, loan-period rule, due date, status changes, observer, adapter |
| White-box / basis path | 5 | all passed | the 5 independent paths of `allocate()` |
| Integration | 17 | all passed | services + real database: full loan and donation, rollback, concurrent approvals |
| Black-box (HTTP API) | 33 | all passed | boundary values, required fields, permission matrix |
| **Backend total** | **83** | **83 passed, 0 failed** | |
| Flutter (unit + widget) | 12 | all passed | JSON parsing, API client, login form |

Code coverage of the backend (JaCoCo): **line 89.9 %, branch 78.4 %**.

**Black-box example — loan period (valid range 1 to 30 days)**

| Input | 0 | 1 | 30 | 31 | −5 | missing |
|---|---|---|---|---|---|---|
| Expected | refused | accepted | accepted | refused | refused | refused |
| Result | pass | pass | pass | pass | pass | pass |

**White-box — basis path testing of `allocate()`**

The method has 4 decisions, so cyclomatic complexity V(G) = 4 + 1 = 5. There are 5 independent
paths and one test for each:

| Path | Situation | Expected result |
|---|---|---|
| 1 | The user is not a coordinator | refused (403) |
| 2 | The item does not exist | not found (404) |
| 3 | The item is already reserved | conflict (409) |
| 4 | No pending request for the item | bad request (400) |
| 5 | Everything is fine | request approved, item reserved, pickup code created |

The control-flow graph, all test tables and the raw outputs are in `docs/TESTING.md` and
`docs/test-evidence/`.

**Defect found by testing and fixed:** when the server was stopped suddenly right after an action,
the last change was lost (the H2 database wrote to disk with a delay). Fixed with the setting
`WRITE_DELAY=0` and checked again by stopping and restarting the server.

## 9. Deployment

```
Android phone  --https-->  Render (Spring Boot in Docker)  -->  Supabase PostgreSQL
```

* The backend runs on Render at `https://shikkhasetu.onrender.com`.
* Data is stored in a PostgreSQL database on Supabase.
* Passwords for the database and the coordinator are stored only as secret settings on Render.
  They are not in the source code.
* User passwords are stored as BCrypt hashes, never as plain text.

## 10. What was verified, and what was not

**Verified**

* All 83 backend tests and 12 Flutter tests pass.
* The live server answers; student registration, login and the permission checks were tested
  against it.
* On a real Android phone, with the live server: installing the APK, registering, logging in as
  student and as coordinator, offering an item, the coordinator approving listings, and sending a
  request.

**Not yet verified**

* On the phone: approve request → handover with pickup code → return (the last steps of the flow).
* The concurrent-approval test ran only on the H2 database, not on PostgreSQL.
* The MySQL configuration was written but never run.
* No test with many real users; no performance test.

## 11. Known limitations

* The free server sleeps after 15 minutes without use. The first request after that can take
  one to two minutes. Open the app a few minutes before a demonstration.
* No item photos, no push notifications (notifications are shown inside the app), Android only.
* Login sessions do not expire until logout.
* Overdue loans are marked, but no automatic reminder is sent.

## 12. Demonstration steps

1. Open the app. Log in as a student (or create a new student account).
2. Catalogue: search and use the filters. Tap **Offer item** and submit a book.
3. Log out. Log in as the coordinator.
4. Catalogue → Status: *Pending review* → **Approve listing**.
5. Requests tab → **Approve** a pending request. The item becomes *Reserved*.
6. Log in as the student → Requests tab shows the **pickup code**; Alerts shows the notification.
7. Coordinator → **Hand over** → type the pickup code → item is *On loan* with a due date.
8. Coordinator → **Record return** → item is *Available* again.
9. Impact tab shows the updated numbers.

## 13. Documents in the project

| File | Content |
|---|---|
| `README.md` | Setup, how to run, API list |
| `docs/PROJECT_REPORT.md` | This report |
| `docs/DESIGN_PATTERNS.md` | The four patterns in detail |
| `docs/TESTING.md` | Test techniques, tables, control-flow graph, defect log |
| `docs/test-evidence/` | Raw test outputs |
| `docs/DEPLOY_CLOUD.md` | How the cloud deployment was done |
| `ShikkhaSetu_Project_Proposal_CSE327_v2_Mobile.docx` | Revised proposal |
