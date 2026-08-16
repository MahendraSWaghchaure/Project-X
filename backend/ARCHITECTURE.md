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

## 6. What this document is not

None of the above makes a single free-tier VM serve literally millions of concurrent users — that
requires more compute, more database capacity, and a real message broker, which are hosting-tier
and cost decisions, not code changes. What this phase delivers is that the code is already
*correct* at each of these bottlenecks, so scaling later means adding infrastructure against
working patterns, not discovering these five problems for the first time under production load.
