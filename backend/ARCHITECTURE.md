# Scale architecture: what's here today, and what swaps in later

This backend runs as a modular monolith on a single instance (currently a free-tier Oracle VM —
see `../DEPLOY.md`). That's the right architecture for its current scale, and the right one for a
while longer. This document exists so that when it stops being right, the next step is adding
infrastructure against already-correct code, not rewriting the code under load.

Every item below follows the same shape: a real bottleneck, the pattern used to address it with
the infrastructure this deployment actually has (Postgres, Redis, one JVM), and the specific thing
that swaps in when a single instance genuinely isn't enough anymore.

## 1. GPS ping ingestion

**Today:** `LocationIngestController` → `LocationIngestService.recordPing()` does the minimum
synchronous work — resolve the active trip, persist the raw ping — then publishes a
`LocationIngestedEvent` and returns. Everything else (geofence checks, safety-flag checks, the
Redis cache update, the live broadcast) happens in `LocationIngestedEventListener`, off the
request thread, on a dedicated bounded pool (`trackingExecutor`, see `AsyncTrackingConfig`).

**Why:** at real volume this is the highest-throughput write path in the whole system by a wide
margin — every vehicle on an active trip pings every ~10 seconds. The ingest endpoint's only job
should be "accept and persist fast."

**Swaps in at scale:** `ApplicationEventPublisher.publishEvent(...)` is the same seam a Kafka
producer occupies. Ingestion becomes a stateless HTTP layer that writes to a Kafka topic
partitioned by `vehicle_id` (for per-vehicle ordering), and `LocationIngestedEventListener`
becomes a Kafka consumer group instead of an in-process `@Async` listener — same method body,
different trigger.

## 2. Cross-instance live updates (WebSocket/STOMP)

**Today:** `TrackingWebSocketConfig` still uses Spring's in-memory `SimpleBrokerMessageHandler` —
correct and simplest for one instance. `LiveLocationBroadcaster` and `SosBroadcaster` publish
through `StompRelayPublisher`, which sends to a Redis pub/sub channel
(`StompRelayConfig.CHANNEL`); every instance runs a `StompRelaySubscriber` that re-broadcasts
whatever it receives to its own locally-connected STOMP sessions.

**Why:** with `enableSimpleBroker`, a parent's WebSocket session and the ping that should update
them have to land on the *same* JVM. Add a second instance behind a load balancer and roughly half
of parents stop getting live updates — not a hypothetical, a certainty, since there's no
coordination between instances at all today. The Redis relay fixes this now, at zero cost while
there's only one instance (the message round-trips through Redis and comes right back).

**Swaps in at scale:** replace `enableSimpleBroker` with `enableStompBrokerRelay` pointing at a
real STOMP-capable broker (RabbitMQ, ActiveMQ), or keep the Redis relay pattern and move it behind
a dedicated WebSocket gateway tier (stateless nodes that only hold client connections, separate
from the nodes doing ingestion/business logic) once connection count alone is the bottleneck.

## 3. Notification dispatch

**Today:** `NotificationDispatchService.dispatch()` is `@Async` on its own bounded pool
(`notificationExecutor`), so a slow Twilio/SMTP call never blocks the event listener that raised
it. `RetryingChannelDispatcher` retries a failed send up to 3 times with exponential backoff
(500ms → 1s → 2s) before giving up; a channel that's simply unconfigured
(`ChannelNotConfiguredException`) is never retried, since retrying can't fix a missing API key.
Every outcome — sent, skipped, or exhausted — is still logged to `NotificationRecord`, now with a
`DEAD_LETTERED` status for the exhausted case, queryable via
`NotificationDispatchService.listDeadLettered()`.

**Why:** the audit-log requirement (every alert, queryable later) already existed and is
untouched. What was missing was resilience — a synchronous, non-retried send meant one flaky
provider response turned into a permanently lost notification with no worklist to catch it.

**Swaps in at scale:** the `@Async` executor is the seam a real queue (SQS, or a Kafka topic
consumed by a dedicated notification-worker deployment) occupies once dispatch volume needs to
scale independently of the rest of the app, or needs delivery guarantees an in-process thread
pool can't give (survives a process restart, visible backlog depth, per-provider rate limiting
enforced centrally instead of per-instance).

## 4. Geofence event dedup at high ping volume

**Today:** `GeofenceEventKeySeenFilter` is a per-instance, in-memory Guava `BloomFilter` in front
of `GeofenceEventRepository`'s exists-then-insert check. A key that's *definitely* never been seen
(`mightContain` returns false) skips straight to raising the event — no need to check first. A key
that *might* have been seen still always falls through to the real database check, which stays the
single source of truth; Bloom filters can false-positive but never false-negative, so this can
never cause a real event to go unraised. Reset daily (events are trip/date-scoped, so nothing
needs to persist across days).

**Why:** stated plainly, this optimization is modest by design — it saves a database round trip
for the guaranteed-first occurrence of each key, not for the repeated-duplicate case (which still
always hits the database, on purpose, for correctness). A more aggressive version that skipped the
database whenever the filter said "might have been seen" would trade a small, real risk of
silently dropping a genuine first-time arrival alert for a bigger performance win — not an
acceptable trade for a safety product. This is deliberately the conservative version.

**Swaps in at scale:** if per-instance memory becomes the constraint (many instances, each with
their own filter, or a filter outgrowing its sizing), the same negative-cache pattern moves to
Redis (a shared probabilistic structure, or a plain `SETNX`-based dedup key with a TTL) so every
instance shares one view instead of each guessing independently.

## 5. Location history storage

**Today:** `vehicle_location_pings` is a native Postgres table partitioned by month
(`RANGE (recorded_at)`, see migration `V34`), with a GiST index on the geometry column, a
`(vehicle_id, recorded_at)` index for the "latest ping" lookup, and a default partition as a
safety net for any row outside the pre-created range.

**Why:** this is the single highest-row-count table in the system. Partitioning means an old
month's data can be dropped or archived to cold storage as a fast metadata operation
(`DETACH PARTITION` + move) instead of a slow row-by-row `DELETE` that competes with live traffic
for I/O. The GiST index isn't fixing an existing slow query (today's geofence checks compare
distances in application code against small, id-keyed lookups, not a spatial SQL query) — it's
there so the next query that *does* need proximity search (nearest-vehicle lookups, historical
path queries) is O(log n) from day one.

**Not automated in this pass:** creating each new month's partition ahead of time is a recurring
ops task — a cron job or manual migration, not code that runs itself. If that lapses, new pings
still succeed (they land in the default partition), just without the monthly-pruning benefit until
backfilled.

**Swaps in at scale:** if a single Postgres instance's write throughput becomes the ceiling (not
just storage/vacuum overhead), this table is the natural first candidate to move to a
purpose-built time-series store, or to a managed Postgres with read replicas serving the
historical-query side while a dedicated primary absorbs writes.

## 6. School attendance and safe-arrival verification (`module-attendance`)

**What it is:** a school roster (`students`, subscribed to Sunju transport or not), classrooms, a
daily attendance sheet, safe-arrival verification (driver event -> school confirmation -> guardian
approval), basic assessments/comments, a `TEACHER` role, and a consent-filtered leads listing for
the platform admin. Flyway range V100-V109. Dependencies are one-way: `module-tracking` only
publishes `ChildTripStatusChangedEvent` (defined in `common`); `module-attendance` depends on
tracking/people/school/identity/notification, never the reverse, so there is no cycle.

**Arrival pipeline:** `ChildTripStatusChangedEvent` (AFTER_COMMIT, `@Async` on the dedicated
`attendanceExecutor`, MDC/`X-Request-Id` carried by `MdcTaskDecorator`) -> `ArrivalService` opens the
`arrival_verifications` row (BOARDED on a morning trip = HOME_PICKUP, DROPPED_AT_SCHOOL = SCHOOL_DROP,
PICKED_FROM_SCHOOL = SCHOOL_PICKUP; idempotent on `(trip_id, child_id, kind)`) -> `ArrivalChangedEvent`
(AFTER_COMMIT) -> `ArrivalNotifier` pushes the Arrival to `/topic/parent/{userId}/arrivals`,
`/topic/school/{id}/arrivals`, `/topic/trip/{id}/arrivals` through the existing Redis STOMP relay, and
sends SMS/email/push (all guardians via `GuardianAccessService.allGuardianIds`, plus school admins)
on a mismatch or dispute. The outcome rule lives in exactly one place, `ArrivalOutcome.compute`.
Arrival rows are `@Version`ed and updated through a retrying transaction (three actors can race).
STOMP SUBSCRIBE to these three topics is authorized in `WebSocketAuthChannelInterceptor` (a parent can
only subscribe to their own user id; staff only to their own school/trip).

**Attendance storage:** `school_attendance` is RANGE-partitioned by month on `attendance_date`
(PK `(id, attendance_date)`, idempotency key `UNIQUE (student_id, attendance_date)`, indexes
`(school_id, attendance_date)` and `(student_id, attendance_date)`), with a DEFAULT partition as a
safety net. It is deliberately not a JPA entity - all access is set-based SQL - so partitioning and
Hibernate `validate` do not interact. `ensure_school_attendance_partitions()` (called by the V100
migration for -2..+12 months and daily by `AttendancePartitionJob` for +6 months) creates partitions
ahead of time; if a month already has rows in the DEFAULT partition Postgres refuses to create it and
the job logs an error (move those rows manually).

**Scale seams, in order of when they bite:**
- *Bulk write:* one `INSERT ... SELECT unnest(...) ON CONFLICT ... DO UPDATE ... WHERE status IS DISTINCT FROM`
  statement per sheet (<= 500 rows, enforced), so a replayed sheet writes nothing. Per-IP rate limits
  (`RateLimitProperties`: `attendance-bulk-per-minute`, `student-import-per-minute`,
  `arrival-respond-per-minute`). Swap-in: put the endpoint behind a queue (Kafka/SQS) and have a worker
  run the same statement.
- *Reads:* every list is size-capped (max page 100; sheet/bulk 500, over-cap = 400, never silent
  truncation); the summary is three indexed COUNTs in one statement, cached in Redis for 15s
  (`projectx.attendance.summary-cache-ttl-seconds`) and evicted on writes. Offset paging is used for the
  student list (fine at school scale); the platform-wide leads listing is the first thing to move to
  keyset paging (`id > :last`) if it grows large. Swap-in: point read-only methods at a read replica.
- *Driver event -> arrival:* the in-process `@Async` hop is the seam a queue/outbox occupies. Known gap:
  a crash between the driver's commit and the async handler loses that one arrival (the driver event is
  still recorded); an outbox table fixes it when it matters.
- *Fan-out:* STOMP via the Redis relay (already multi-instance safe); notifications via
  `NotificationDispatchService` (its own pool, retries, dead-letter log).
- *Text search* (`q` on students) is `lower(...) LIKE`, indexed only by `(school_id, classroom_id)`; add
  `pg_trgm` GIN indexes if a single school ever has tens of thousands of students.

`loadtest/attendance.js` (k6) measures real req/s on whatever box it is pointed at. It measures; it
does not prove billion-scale.

**Compliance:** students are minors. `marketing_consent` defaults to false, the leads query filters
`marketing_consent = true AND child_id IS NULL` inside the repository method (no unfiltered path),
consent changes and every leads listing are audit-logged. Get legal review (India DPDP Act) before
any outreach.

## 7. Phase 2: platform-owned payments, teacher scoping, guardian consent, public intake

**Payments belong to the platform (B1/B13).** Fee schedules, fee invoices, school payment listings, payroll listings, the
incentive setter and the whole `/billing/**` admin controller are `PLATFORM_ADMIN`-only; a `SCHOOL_ADMIN` gets 403 on all of
them and `GET /schools/{id}/dashboard-stats` returns `revenueThisMonth = null`, `payrollPending = payrollFailed = 0` for them.
Parents still pay the platform (Razorpay) and drivers still see their own payroll. `PayrollService.listForSchool` no longer
scans the whole table: it reads the newest 500 rows of the school's drivers in SQL.

**Teacher scoping is enforced in one place, `TeacherScope`.** A teacher is class teacher of at most one classroom
(`classrooms.class_teacher_id`, DB-unique partial index) and lectures in the classrooms where they have
`timetable_periods`. CLASS_TEACHER: full student details incl. guardian contacts, attendance, comments, assessments of that
class. LECTURER: assessments only; guardian fields and consent fields are removed from the student payload. No relation: 403.
School-wide arrival boards (REST and the `/topic/school|trip/**` STOMP topics) are admin-only; teachers get their own
class alerts instead.

**Teacher alerts (C4).** `ArrivalNotifier.fanOut` (already async on `attendanceExecutor`, MDC-carried) asks
`TeacherAlertService.onArrivalChanged`; when a SCHOOL_DROP arrival is `school_confirm = ABSENT` it pushes one alert to
`/topic/teacher/{teacherUserId}/alerts` through the existing Redis relay (JSON in, `byte[]` out in `StompRelaySubscriber`).
A Redis `SET NX` marker per arrival (24 h) de-duplicates repeated changes and is cleared when the school corrects the mark.
`WebSocketAuthChannelInterceptor` treats everything under `/topic/teacher/` as deny-by-default: the destination must be exactly
`/topic/teacher/{positive id}/alerts`, the session must be a TEACHER and the id must be their own.

**Guardian consent (A2)** is a single-use, 48 h SMS link. Only the SHA-256 of the 256-bit token is stored; the SMS body (which
contains the token) is never written to `notification_records` (the log row gets a redacted body and a masked number).
Opening the link changes nothing; the answer is one atomic conditional `UPDATE ... WHERE used_at IS NULL AND expires_at > now()`
so concurrent answers cannot both win. Every failure on the public side is the same generic `410`. A school never receives the
link (`projectx.consent.expose-dev-link` is a dev/test-only switch, default false). Public endpoints
(`/public/consent/*`, `/public/consent/*/respond`, `/public/inquiries`) are rate limited per IP by `RateLimitFilter`
(`consent-view-per-minute` 30, `consent-respond-per-minute` 10, `inquiry-per-minute` 5). The known `X-Forwarded-For` trust issue
of that filter is unchanged and still needs a proxy that overwrites the header.

**Public inquiries (E4)** live in `module-notification` (`inquiries` table, `POST /public/inquiries`, honeypot field, platform
admins notified AFTER_COMMIT through the existing dispatcher).

**Cross-module seams added.** `module-school` cannot call `module-people` (the dependency points the other way), so vehicle
driver/student data goes through the `VehicleDriverGateway` interface implemented in `module-people`; `module-grievance` stays
a leaf and gets display names and link verification from the `GrievanceDirectory` interface implemented in `app`.

## 8. What this document is not

None of the above makes a single free-tier VM serve literally millions of concurrent users — that
requires more compute, more database capacity, and a real message broker, which are hosting-tier
and cost decisions, not code changes. What this phase delivers is that the code is already
*correct* at each of these bottlenecks, so scaling later means adding infrastructure against
working patterns, not discovering these five problems for the first time under production load.
