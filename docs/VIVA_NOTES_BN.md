# Viva notes (বাংলায়) — ShikkhaSetu

Teacher-কে দেখানোর আগে এটা একবার পড়ে নাও। প্রতিটি উত্তরের সাথে কোন file খুলে দেখাবে তাও লেখা আছে।

## ১. এক লাইনে project

"ShikkhaSetu একটি Android app, যেখানে student-রা বই ও calculator দান বা ধার দিতে ও নিতে পারে, আর
club-এর coordinator সেগুলো অনুমোদন, হস্তান্তর ও ফেরত নথিভুক্ত করে।"

## ২. কী দিয়ে বানানো

| অংশ | প্রযুক্তি |
|---|---|
| Mobile app | Flutter (Dart) — folder `mobile/` |
| Backend server | Java 17, Spring Boot — folder `backend/` |
| Database | Local-এ H2, cloud-এ PostgreSQL (Supabase) |
| Hosting | Render — `https://shikkhasetu.onrender.com` |

**"React-এর বদলে Flutter কেন?"** → Proposal-এ web ছিল, পরে requirement হলো mobile app; তাই frontend
Flutter-এ করা হয়েছে। Backend-এর পরিকল্পনা (Spring Boot) একই আছে। পরিবর্তনগুলো v2 proposal-এর
section 0-তে লেখা।

**"MySQL-এর বদলে H2/PostgreSQL কেন?"** → Code একই (JPA), শুধু configuration আলাদা। H2-তে আলাদা
server install লাগে না; cloud-এ free PostgreSQL পাওয়া যায়।

## ৩. চারটি design pattern

| Pattern | এক লাইনে | যে file দেখাবে |
|---|---|---|
| Factory Method | Loan আর donation-এর নিয়ম আলাদা; creator class ঠিক করে কোন workflow object তৈরি হবে | `workflow/WorkflowCreator.java`, `LoanWorkflow.java`, `DonationWorkflow.java` |
| Strategy | একই item-এ অনেকে request করলে কে পাবে — "আগে এলে আগে" অথবা "coordinator-এর পছন্দ"; নিয়মটা বদলানো যায় | `allocation/AllocationPolicy.java` |
| Observer | Approve হলে notification ও audit log নিজে থেকে তৈরি হয়; service জানে না কে শুনছে | `event/EventBus.java`, `NotificationListener.java`, `AuditLogListener.java` |
| Adapter | আমাদের code বলে `send(user, message)`, email provider চায় `deliver(address, subject, body)`; adapter মাঝখানে রূপান্তর করে | `notify/EmailNotificationAdapter.java` |

সৎভাবে বলবে: email provider-টি একটি stub (console-এ print করে), আসল email যায় না।

## ৪. সবচেয়ে গুরুত্বপূর্ণ প্রশ্ন: একই item দুজনকে দেওয়া আটকায় কীভাবে?

File: `service/RequestService.java`, method `allocate()`।

1. পুরো method একটি database **transaction**।
2. Item-এর row **lock** করে পড়া হয় (`findByIdForUpdate`)। দ্বিতীয় approval অপেক্ষা করে।
3. অপেক্ষার পর দ্বিতীয়টি দেখে item আগেই **Reserved**, তাই প্রত্যাখ্যাত হয় (409)।
4. বাড়তি নিরাপত্তা: item-এ `@Version` column।

প্রমাণ: `WorkflowIntegrationTest.concurrentApprovals_produceExactlyOneAllocation` — দুটি thread একসাথে
approve করে, 10 বার; প্রতিবার ঠিক একটি সফল।

## ৫. Testing

| ধরন | সংখ্যা | কী |
|---|---|---|
| Unit | 28 | ছোট ছোট নিয়ম আলাদাভাবে (policy, due date, status) |
| White-box / basis path | 5 | `allocate()`-এর 5টি path |
| Integration | 17 | Service + আসল database একসাথে |
| Black-box | 33 | শুধু HTTP API দিয়ে; boundary value ও permission |
| মোট backend | 83 | সব pass |
| Flutter | 12 | সব pass |

**Basis path:** `allocate()`-এ 4টি decision (`if`) আছে → cyclomatic complexity = 4 + 1 = **5** →
5টি independent path → 5টি test (`AllocateBasisPathTest.java`)। Graph আছে `docs/TESTING.md`-তে।

**Boundary value:** loan period-এর valid সীমা 1–30 দিন। Test করা মান: 0 ✗, 1 ✓, 30 ✓, 31 ✗।

**Black-box আর white-box-এর পার্থক্য:** black-box-এ code না দেখে শুধু input/output পরীক্ষা;
white-box-এ code-এর ভেতরের প্রতিটি পথ পরীক্ষা।

Coverage: line 89.9%, branch 78.4%।

## ৬. Security নিয়ে প্রশ্ন এলে

* Password database-এ BCrypt hash হিসেবে থাকে, আসল password নয় (`AuthService.java`)।
* Login করলে server একটি random token দেয়; প্রতিটি request-এ app সেটা পাঠায় (`AuthInterceptor.java`)।
* Permission server-এ পরীক্ষা হয়, app-এ নয়। Student approve করতে চাইলে server 403 দেয়।
* Pickup code শুধু যে student request করেছে সে-ই দেখতে পায়।

## ৭. যা হয়নি — জিজ্ঞেস করলে সরাসরি বলবে

* Item-এর ছবি, push notification, iOS version নেই।
* Email আসলে পাঠানো হয় না (stub)।
* MySQL profile লেখা আছে কিন্তু চালানো হয়নি।
* অনেক user নিয়ে performance test হয়নি।

## ৮. Demo-র আগে

* Free server 15 মিনিট ব্যবহার না হলে ঘুমিয়ে যায়; জাগতে এক-দুই মিনিট লাগে। Teacher-এর কাছে যাওয়ার
  **৫ মিনিট আগে** app খুলে একবার login করে নাও। প্রথমবার "did not answer in time" এলে আবার চাপো।
* Coordinator-এর email ও password মনে রাখো।
* Demo-র ধাপ: `docs/PROJECT_REPORT.md`-এর section 12।
