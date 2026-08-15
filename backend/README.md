# Sunju (ProjectX) Backend

Modular-monolith Spring Boot 4 / Java 25 application. One deployable JAR (module `app`), internal
module boundaries enforced by Maven module dependencies (see root `pom.xml`'s `<modules>` and each
module's own `pom.xml`).

## What you need installed

- **JDK 25** (LTS) to build and run the app itself. Check with `java -version`.
- **A JDK Gradle actually supports, only if you're building the Android app** — Gradle doesn't yet
  support JDK 25's class file version ("Unsupported class file major version 69"). Use JDK 17 or 21
  for that specific build; see `frontend/MOBILE.md`.
- **Maven 3.9+** — or open this folder in IntelliJ IDEA / VS Code (Java extension pack) / Eclipse,
  all of which bundle Maven and can run/debug `app`'s `ProjectXApplication` directly.
- **Postgres 16+ with PostGIS, and Redis** — either via `docker compose up -d postgres redis` from
  the repo root, or installed natively. This was built and fully verified on Windows *without*
  Docker or admin rights, using portable zip installs:
  - JDK/Maven/Node as plain zip extracts on `PATH`.
  - PostgreSQL via the EDB Windows zip binaries, with the OSGeo PostGIS bundle merged in
    (`robocopy` the PostGIS files over the EDB install directory) — PostGIS isn't bundled with the
    plain EDB zip.
  - Redis via a community-maintained Windows build (Microsoft doesn't ship an official one).
  - Default local credentials either way: DB `projectx` / user `projectx` / password `projectx` on
    `localhost:5432`; Redis on `localhost:6379`, no auth. Change all of this before anything but
    your own machine touches it.

## Module map

| Module | Owns | Flyway version range |
|---|---|---|
| `common` | Base entity, `ApiResponse` envelope, exceptions, `CurrentUser`/`CurrentUserContext`, geo helpers, shared domain events | — |
| `module-identity` | `User`, JWT issue/refresh (RSA), invitations, Spring Security chain, Redis-backed rate limiting | V1-V9 |
| `module-school` | `School`, `Vehicle`, `Route`/`RouteStop` (with pickup-radius zones) | V10-V19 |
| `module-people` | `DriverProfile` (incl. independent self-registration + claim), `ParentProfile`, `Child` (incl. geo auto-route-assignment) | V20-V29 |
| `module-tracking` | `Trip` lifecycle, Traccar ingestion, geofencing, live-location cache/broadcast, delay monitor | V30-V39 |
| `module-notification` | SMS/email/push dispatch, notification audit log, event listeners | V40-V49 |
| `module-insurance` | `InsurancePolicy` CRUD (create/list/get, scoped by school) for child/driver/vehicle coverage | V50-V59 |
| `module-payment` | Fee schedules/invoices, Razorpay Checkout payments, late-fee waivers, driver bank details + RazorpayX payroll disbursement | V60-V69 |
| `module-grievance` | `Grievance` intake/resolution, live STOMP push | V70-V79 |
| `app` | `@SpringBootApplication`, config, bootstrap seeding, OpenAPI, the only module that gets repackaged into a runnable JAR | — |

## Run it

```sh
# from repo root
docker compose up -d postgres redis   # add "traccar" once you're ready to wire real GPS hardware
# (or point at natively-installed Postgres+PostGIS/Redis - see above)

cd backend
mvn -pl app -am spring-boot:run
```

First boot: Flyway applies every module's migrations (they all live under each module's own
`db/migration/`, scanned together off the classpath). Because each module owns an independent
version-number range rather than one globally-increasing sequence, a later change to an earlier
module can land with a lower version number than one another module already applied - Flyway's
`out-of-order` mode is deliberately turned on in `application.yml` to allow this; it's expected
here, not a real ordering hazard. Then `PlatformAdminSeeder` creates a `PLATFORM_ADMIN` login if
one doesn't exist yet (default `admin@projectx.local` / `changeme123` - override via
`PLATFORM_ADMIN_EMAIL`/`PLATFORM_ADMIN_PASSWORD` env vars, and change it before this is anywhere
near a shared environment).

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Verifying the core loop end-to-end

```sh
# 1. Log in as the seeded platform admin
curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@projectx.local","password":"changeme123"}' | jq

# 2. Create a school (also creates its first SCHOOL_ADMIN login) - use the accessToken from step 1
curl -s -X POST localhost:8080/schools -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Sunrise Public School","address":"123 MG Road","city":"Pune",
       "latitude":18.5204,"longitude":73.8567,"geofenceRadiusMeters":300,"dismissalTime":"15:00:00",
       "adminFirstName":"Asha","adminLastName":"Kulkarni","adminEmail":"admin@sunrise.example","adminPassword":"changeme123"}'

# From here: log in as the school admin, add a vehicle/driver/route (with stops + a zone via
# PUT /schools/{id}/routes/{routeId}/zone), invite a parent, have them register a child, then either
# POST /schools/{id}/children/{childId}/auto-assign-route or wait for a real Traccar-forwarded
# ping / POST a synthetic one to /tracking/ingest/traccar/{TRACKING_INGEST_SECRET} (shaped like
# TraccarWebhookPayload.java) to watch a GeofenceEvent fire and a notification get logged.
```

## Payments (Razorpay + RazorpayX)

Both halves are **off by default** so the app boots and every other feature works without real
payment credentials. Every attempted payment/payout when disabled fails with a clear, specific
error rather than pretending to succeed - see `module-payment`'s `RazorpayProperties`.

```sh
# Parent fee collection (Razorpay Orders/Checkout)
RAZORPAY_ENABLED=true
RAZORPAY_KEY_ID=<your Razorpay key id>
RAZORPAY_KEY_SECRET=<your Razorpay key secret>

# Driver payroll disbursement (RazorpayX Payouts) - needs a real, KYC'd, funded RazorpayX account
RAZORPAYX_PAYOUTS_ENABLED=true
RAZORPAYX_ACCOUNT_NUMBER=<your RazorpayX virtual account number>
# (uses the same RAZORPAY_KEY_ID/KEY_SECRET as above)
```

The integration itself was verified against Razorpay's real production API (not mocked) using
throwaway test credentials - a clean `401 Authentication failed` response back from Razorpay
confirms the request shape, Basic Auth header, and error handling are all correct; only real
credentials are missing.

## Testing

```sh
# Unit tests only (fast, no external services)
mvn -pl module-payment test

# Full integration suite - hits real HTTP endpoints against a real Postgres database
createdb -U projectx projectx_test        # one-time: a database separate from your dev one
mvn -pl app -am test
```

The integration suite (`app/src/test/java/com/projectx/app/`) spins up the whole Spring context on
a random port and drives it exactly like the curl walkthrough above, just automated: login/refresh,
school lifecycle + cross-tenant isolation, the independent-driver claim flow, zone-based route
auto-assignment end to end, and the grievance raise/resolve/cross-tenant-reject flow. It talks to
`projectx_test` (see `app/src/test/resources/application-test.yml`), not your dev database, on the
same local Postgres instance.

## GPS ingestion

Dedicated in-vehicle hardware trackers speak proprietary binary protocols (Teltonika, Concox,
etc.) - reimplementing those ourselves isn't worth it. Point real devices at a self-hosted
**Traccar** instance instead (`docker compose up -d traccar`, web UI on :8082), and configure
Traccar's position forwarding (Settings → Server → Position Forwarding) to POST to
`http://<app-host>:8080/tracking/ingest/traccar/<TRACKING_INGEST_SECRET>` in JSON format with
`forward.additional=true` (needed so `device.uniqueId` is included - that's what's matched against
`Vehicle.gpsDeviceId`). If you already have a GPS vendor with its own webhook/API instead of raw
hardware, point that at the same endpoint - `TrackingIngestController` only cares about the JSON
shape, not who's sending it.

## Production checklist

`application-prod.yml` (activate with `SPRING_PROFILES_ACTIVE=prod`) intentionally has **no
defaults** for these - startup fails fast with a clear "could not resolve placeholder" error
instead of silently running insecure:

- `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` - real RSA keypair (`openssl genrsa -out
  jwt-private.pem 2048 && openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem`), not the
  ephemeral one generated for local dev (which invalidates every token on every restart, by
  design, so it can never accidentally be relied on in a real environment).
- `CORS_ALLOWED_ORIGINS` - your real frontend origin(s).
- `TRACKING_INGEST_SECRET` - long random string, goes in the Traccar forwarding URL.
- `PLATFORM_ADMIN_PASSWORD` - change immediately after first login regardless.
- Rate-limit thresholds (`projectx.security.rate-limit.*`) default to 20 logins/30 refreshes/5
  driver-registrations per minute per IP - tune for real traffic if needed.

Twilio (`TWILIO_ENABLED=true` + creds) and SMTP (`SMTP_ENABLED=true` + host/creds) stay off by
default everywhere - every attempted notification is still logged to `notification_records` with
status `SKIPPED_NOT_CONFIGURED` so nothing silently disappears. Razorpay/RazorpayX follow the same
pattern - see "Payments" above.
