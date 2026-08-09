# ProjectX Backend

Modular-monolith Spring Boot 4 / Java 25 application. One deployable JAR (module `app`), internal
module boundaries enforced by Maven module dependencies (see root `pom.xml`'s `<modules>` and each
module's own `pom.xml`).

## What you need installed

This was built without a local Maven/Node/Docker toolchain available to verify against, so please
run the verification steps below yourself once these are installed:

- **JDK 25** (LTS). Check with `java -version`.
- **Maven 3.9+** — or open this folder in IntelliJ IDEA / VS Code (Java extension pack) / Eclipse,
  all of which bundle Maven and can run/debug `app`'s `ProjectXApplication` directly without a
  separate install.
- **Docker** (for Postgres+PostGIS, Redis, and optionally Traccar via the root `docker-compose.yml`)
  — or install Postgres 16+ with the PostGIS extension and Redis natively if you'd rather not use
  Docker.

## Module map

| Module | Owns | Flyway version range |
|---|---|---|
| `common` | Base entity, `ApiResponse` envelope, exceptions, `CurrentUser`/`CurrentUserContext`, geo helpers, shared domain events | — |
| `module-identity` | `User`, JWT issue/refresh (RSA), invitations, Spring Security chain | V1-V9 |
| `module-school` | `School`, `Vehicle`, `Route`/`RouteStop`, `SchoolAdminProfile` | V10-V19 |
| `module-people` | `DriverProfile`, `ParentProfile`, `Child` | V20-V29 |
| `module-tracking` | `Trip` lifecycle, Traccar ingestion, geofencing, live-location cache/broadcast, delay monitor | V30-V39 |
| `module-notification` | SMS/email/push dispatch, notification audit log, event listeners | V40-V49 |
| `module-insurance` | `InsurancePolicy` (data model only - see module's pom.xml description) | V50-V59 |
| `module-payment` | `PayrollRecord` (data model only - no Stripe/service layer yet) | V60-V69 |
| `app` | `@SpringBootApplication`, config, bootstrap seeding, the only module that gets repackaged into a runnable JAR | — |

## Run it

```sh
# from repo root
docker compose up -d postgres redis   # add "traccar" once you're ready to wire real GPS hardware

cd backend
mvn -pl app -am spring-boot:run
```

First boot: Flyway applies every module's migrations (they all live under each module's own
`db/migration/`, scanned together off the classpath - see `app/src/main/resources/application.yml`
for why no explicit `spring.flyway.locations` override is needed), then `PlatformAdminSeeder`
creates a `PLATFORM_ADMIN` login if one doesn't exist yet (default `admin@projectx.local` /
`changeme123` - override via `PLATFORM_ADMIN_EMAIL/PASSWORD` env vars, and change it before this
is anywhere near a shared environment).

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

# From here: log in as the school admin, add a vehicle/driver/route (with stops), schedule a trip,
# and either wait for a real Traccar-forwarded ping or POST a synthetic one to
# /tracking/ingest/traccar/{TRACKING_INGEST_SECRET} shaped like TraccarWebhookPayload.java to watch
# a GeofenceEvent fire and a notification get logged (GET /notifications/me as the affected parent).
```

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
  ephemeral one generated for local dev.
- `CORS_ALLOWED_ORIGINS` - your real frontend origin(s).
- `TRACKING_INGEST_SECRET` - long random string, goes in the Traccar forwarding URL.
- `PLATFORM_ADMIN_PASSWORD` - change immediately after first login regardless.

Twilio (`TWILIO_ENABLED=true` + creds) and SMTP (`SMTP_ENABLED=true` + host/creds) stay off by
default everywhere - every attempted notification is still logged to `notification_records` with
status `SKIPPED_NOT_CONFIGURED` so nothing silently disappears.
