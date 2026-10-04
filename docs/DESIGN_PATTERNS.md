# Design patterns in ShikkhaSetu

All four patterns live in the backend (`backend/src/main/java/bd/bubt/shikkhasetu`), because that is
where the business rules are. Each section says **what problem** the pattern solves here, **where the
code is**, and **how to explain it** in a viva.

---

## 1. Factory Method — loan vs. donation workflow

**Problem.** A loan and a donation follow different rules:

| | Loan | Donation |
|---|---|---|
| Loan period | required, 1–30 days | not used |
| After handover | item `ON_LOAN`, due date set | item `DONATED`, no due date |
| Return | item becomes `AVAILABLE` again | not allowed |

Without a pattern, `RequestService` would be full of `if (mode == LOAN) … else …`.

**Code.** Package `workflow`:

| Role in the pattern | Class |
|---|---|
| Product (interface) | `RequestWorkflow` |
| Concrete products | `LoanWorkflow`, `DonationWorkflow` |
| Creator (abstract, declares the factory method `createWorkflow()`) | `WorkflowCreator` |
| Concrete creators (override the factory method) | `WorkflowCreator.LoanWorkflowCreator`, `WorkflowCreator.DonationWorkflowCreator` |
| Client | `RequestService.workflowFor(item)` |

```java
// RequestService – the only place that asks for a workflow
RequestWorkflow workflow = WorkflowCreator.forMode(item.getMode()).workflow();
workflow.handOver(request, today);   // the service does not know which class this is
```

**How to explain it.** "The creator class has an abstract method `createWorkflow()`. Each subclass
decides which object to create. The service only uses the `RequestWorkflow` interface. To add a new
kind of exchange (for example a *swap*), I add one workflow class and one creator; `RequestService`
does not change."

**Tests.** `unit/WorkflowTest`.

---

## 2. Strategy — who gets the item

**Problem.** Several students can request the same item. The club may want *first come, first
served*, or the coordinator may want to choose one student. The approval steps are the same; only
the selection rule changes.

**Code.** Package `allocation`:

| Role | Class |
|---|---|
| Strategy (interface) | `AllocationPolicy` — `select(pendingRequests, chosenRequestId)` |
| Concrete strategies | `AllocationPolicy.FirstComeFirstServed`, `AllocationPolicy.CoordinatorChoice` |
| Context | `RequestService.allocate(actor, itemId, policy, chosenRequestId)` |

The strategy is chosen by the API endpoint that is called:

| Endpoint | Strategy |
|---|---|
| `POST /api/requests/{id}/approve` | `CoordinatorChoice` (this exact request) |
| `POST /api/items/{id}/allocate-fcfs` | `FirstComeFirstServed` (oldest pending request) |

In the app these are the **Approve** button on a request and the **Approve first-come request**
button on a catalogue item.

**How to explain it.** "`allocate()` receives the policy as a parameter. It locks the item, asks the
policy which request wins, and reserves the item. The algorithm for *choosing* is separated from the
algorithm for *reserving*, so I can add a new rule (for example 'student with fewest past loans')
without touching the locking code."

**Tests.** `unit/AllocationPolicyTest`; used end-to-end in
`integration/WorkflowIntegrationTest.donation_fullLifecycle…`.

---

## 3. Observer — notifications and audit log

**Problem.** When a request is approved, the student must be notified and the action must be written
to the audit log. Later we may also want e-mail or statistics. The service should not know about all
of these.

**Code.** Package `event`:

| Role | Class |
|---|---|
| Subject | `EventBus` — `subscribe()`, `unsubscribe()`, `publish()` |
| Observer (interface) | `DomainEventListener` — `onEvent(DomainEvent)` |
| Concrete observers | `NotificationListener`, `AuditLogListener` |
| Event object | `DomainEvent` (type, item, request, actor) |

```java
// RequestService.allocate()
eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_APPROVED, item, chosen, actor));
```

Events: `ITEM_SUBMITTED`, `ITEM_LISTING_APPROVED`, `ITEM_LISTING_REJECTED`, `REQUEST_SUBMITTED`,
`REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_CANCELLED`, `ITEM_HANDED_OVER`, `ITEM_RETURNED`.

**Design decision.** Observers run *inside the same database transaction* as the action. If an
observer fails, the whole action is rolled back, so we never have "approved but nobody was told".
This is proven by `integration/WorkflowIntegrationTest.failureInAnObserver_rollsBackTheWholeApproval`.

**How to explain it.** "The service publishes an event and does not know who listens. Spring gives
the `EventBus` every bean that implements `DomainEventListener`. To add a new reaction I write one
new listener class; no existing class changes."

**Tests.** `unit/ObserverAndAdapterTest`, plus the integration tests above.

---

## 4. Adapter — optional e-mail channel

**Problem.** The application wants to say `send(user, message)`. An external e-mail provider has a
different interface: `deliver(toAddress, subject, htmlBody)`. We cannot change the provider's class.

**Code.** Package `notify`:

| Role | Class |
|---|---|
| Target (what the app uses) | `NotificationChannel` — `send(User, String)` |
| Adaptee (incompatible, "third party") | `ExternalEmailClient` — `deliver(String, String, String)` |
| Adapter | `EmailNotificationAdapter implements NotificationChannel` |
| Other implementation of the target | `InAppNotificationChannel` (saves to the database) |
| Client | `NotificationService` (loops over all enabled channels) |

> **Honest note.** `ExternalEmailClient` is a **stub**: it prints to the console and keeps the
> messages in a list. No real e-mail is sent. The channel is off by default
> (`app.email.enabled=false`). The proposal lists e-mail as optional; the pattern is what is
> demonstrated here.

**How to explain it.** "The adapter implements our interface and, inside, calls the provider's
method with converted arguments (user → e-mail address, message → subject + HTML body). If we buy a
real e-mail service, only `ExternalEmailClient` is replaced."

**Tests.** `unit/ObserverAndAdapterTest.adapter_translatesANotificationIntoAnEmailDelivery`.

---

## Patterns that were considered and not used

| Pattern | Decision |
|---|---|
| Composite (nested categories) | Not used. Three flat categories (book, calculator, other) are enough. |
| Singleton | Not written by hand. Spring already creates one instance of each service. |
| State (item status) | Not used. The transitions are few and are handled by the two workflow classes. |

---

## How one approval flows through all four patterns

```
App: coordinator taps "Approve"
  -> POST /api/requests/7/approve                         (ApiController)
  -> RequestService.approve()  picks CoordinatorChoice     [STRATEGY]
  -> RequestService.allocate() locks item, policy.select(), item -> RESERVED
  -> eventBus.publish(REQUEST_APPROVED)                    [OBSERVER]
       -> NotificationListener -> NotificationService
            -> InAppNotificationChannel  (saved to DB)
            -> EmailNotificationAdapter  (only if enabled) [ADAPTER]
       -> AuditLogListener (saved to DB)
  -> transaction commits

Later: coordinator taps "Hand over"
  -> RequestService.handOver()
  -> WorkflowCreator.forMode(item.mode).workflow()         [FACTORY METHOD]
       LoanWorkflow.handOver()      -> ON_LOAN + due date
       DonationWorkflow.handOver()  -> DONATED
```
