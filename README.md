# ProjectX — School Transport, Accounted For

50-60% of Indian parents put their kids on privately-run school vans/buses with essentially zero
visibility into where the vehicle is or whether it ran on time. ProjectX makes that accountable:
live GPS tracking (Swiggy/Zomato-style), arrival and delay alerts, driver/vehicle/child insurance
records, and a payment/payroll layer — for four kinds of users (parents, drivers, school admins,
and us as the platform operator) plus the schools they belong to.

This was a solo-built prototype for a year (Spring Boot 2.7, no real domain model, placeholder
frontend pages). It's now been rebuilt from the ground up as an enterprise-track modular monolith:
**Java 25 + Spring Boot 4 + Spring Security 7** on the backend, **React 19 + TypeScript + Vite**
on the frontend. See `backend/README.md` and `frontend/README.md` for the details of each side.

## Repo layout

```
backend/     Multi-module Maven project - see backend/README.md for the module map
frontend/    React + TS + Vite - see frontend/README.md
docker-compose.yml   Postgres+PostGIS, Redis, Traccar (GPS protocol normalization)
```

## Quick start

```sh
docker compose up -d postgres redis

cd backend && mvn -pl app -am spring-boot:run   # http://localhost:8080, Swagger at /swagger-ui.html

# separate terminal
cd frontend && npm install && npm run dev        # http://localhost:5173
```

Log in as the seeded platform admin (`admin@projectx.local` / `changeme123` by default — see
`backend/README.md` for how to override and why you must before this goes anywhere near a shared
environment) to onboard your first school.

**This machine had no Maven, Node, or Docker installed** when this was built, so none of the above
has actually been run and verified end-to-end yet — please run it and report back anything that
doesn't come up cleanly.

## What's real vs. what's a stub

Built and functional this pass: identity/auth (JWT + refresh, role-based access), school/vehicle/
route/driver/parent/child onboarding (including the invite-a-parent flow), trip lifecycle (driver
starts/completes a trip, marks children boarded/dropped), live GPS ingestion via a Traccar webhook,
geofence-based arrival/approaching/end-of-day-delay detection, notification dispatch (SMS via
Twilio / email via SMTP, both off by default locally) with a full delivery audit log, and a
parent-facing live tracking map with a home-arrival confirmation button.

Deliberately left as data-model-only stubs (see each module's `pom.xml` description for why):
insurance policy *workflows* (renewal/expiry escalation — the policy records and linkage to
child/driver/vehicle exist), Stripe parent-fee billing, and driver payroll/overtime-incentive
computation. These were called out as phase 2 when the build plan was agreed, precisely so the
core tracking/alert loop could be built deep rather than everything built shallow.

## Architecture decisions worth knowing about

- **Modular monolith, not microservices** — one deployable, Maven-module-enforced boundaries.
  Splittable later if a specific module (most likely `module-tracking`) needs to scale
  independently.
- **GPS hardware via Traccar** — dedicated in-vehicle trackers speak proprietary protocols;
  Traccar (self-hosted, open source) normalizes ~170 of them and forwards clean JSON to our
  ingestion webhook, rather than us reimplementing device protocols.
- **PostgreSQL + PostGIS** — one relational store, native geospatial queries power geofencing.
- **Cross-module references are ids, not JPA relationships** — e.g. `Vehicle.insurancePolicyId` is
  a plain `Long`, not a `@ManyToOne` into `module-insurance`'s entity. Keeps modules independently
  extractable into real services later without an entity-graph untangling project.
