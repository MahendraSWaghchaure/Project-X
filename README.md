# Sunju — School Transport, Accounted For

50-60% of Indian parents put their kids on privately-run school vans/buses with essentially zero
visibility into where the vehicle is or whether it ran on time. Sunju makes that accountable:
live GPS tracking (Swiggy/Zomato-style), arrival and delay alerts, driver/vehicle/child insurance
records, a grievance system, and a full payment/payroll layer — for four kinds of users (parents,
drivers, school admins, and us as the platform operator) plus the schools they belong to. Ships as
a responsive web app and, via Capacitor, a real Android app (an iOS build needs a Mac).

This was a solo-built prototype for a year (Spring Boot 2.7, no real domain model, placeholder
frontend pages). It's since been rebuilt from the ground up and run/verified end-to-end on this
machine: **Java 25 + Spring Boot 4 + Spring Security 7** on the backend, **React 19 + TypeScript +
Vite** on the frontend. See `backend/README.md` and `frontend/README.md` for the details of each
side, and `frontend/MOBILE.md` for the Android/iOS build.

## Repo layout

```
backend/     Multi-module Maven project - see backend/README.md for the module map
frontend/    React + TS + Vite (+ android/, ios/ Capacitor projects) - see frontend/README.md
docker-compose.yml   Postgres+PostGIS, Redis, Traccar - one option for local infra (see below)
```

## Quick start

Postgres+PostGIS and Redis can come from Docker **or** native installs - both were used and
verified while building this (native, since Docker wasn't available on this particular machine;
see `backend/README.md` for exact portable-install steps if you're in the same boat on Windows).

```sh
docker compose up -d postgres redis      # or install Postgres 16+/PostGIS + Redis natively

cd backend && mvn -pl app -am spring-boot:run   # http://localhost:8080, Swagger at /swagger-ui.html

# separate terminal
cd frontend && npm install && npm run dev        # http://localhost:5173
```

Log in as the seeded platform admin (`admin@projectx.local` / `changeme123` by default — see
`backend/README.md` for how to override and why you must before this goes anywhere near a shared
environment) to onboard your first school.

## What's built

Everything below has been run and exercised end-to-end on this machine (curl for the backend,
Playwright for the frontend), not just written and assumed to work:

- **Identity & access** — JWT auth (self-issued RSA, refresh rotation), role-based access for all
  four roles + platform admin, Redis-backed rate limiting on login/refresh/driver-registration,
  baseline security headers (HSTS, X-Frame-Options, nosniff, referrer-policy).
- **Onboarding & fleet management** — full CRUD (create/edit/suspend or deactivate) for schools,
  vehicles, drivers, parents, and children, all tenant-scoped by school. Independent
  (privately-owned-vehicle) drivers can self-register and get "claimed" by a school, which
  transfers their login, profile, and vehicle in one step.
- **Live tracking** — Traccar-normalized GPS ingestion, PostGIS geofencing, a Redis-backed
  latest-position cache, and a STOMP/WebSocket broadcast that drives a live parent-facing map.
- **Trip lifecycle & alerts** — driver starts/completes a trip and marks each child
  boarded/dropped; parent confirms home arrival; geofence-triggered
  approaching/arrived/end-of-day-delay notifications (SMS via Twilio / email via SMTP, both off by
  default locally) with a full delivery audit log.
- **Geo-based route assignment** — routes carry a pickup-radius zone (e.g. 0-5km / 5-10km); a
  child is auto-assigned to whichever zone covers the straight-line distance from home to school,
  then the nearest stop on that route.
- **Grievances** — parents, drivers, and school admins can raise one (category/subject/
  description); school or platform admins resolve it, with the update pushed live over STOMP
  instead of the parent having to refresh.
- **Payments & payroll** (`module-payment`, Razorpay + RazorpayX) — monthly fee invoices due the
  7th with a ₹50/day late fee after that; parents pay via Razorpay Checkout (UPI/QR/cards/
  netbanking/wallets all come from that one widget); prepaying 3/6/12 months earns a 5/8/12%
  discount; parents can request a late-fee waiver with an emergency reason, reviewed by the
  platform admin; drivers add bank details and get paid on the 10th via real RazorpayX Payouts
  calls, recorded with a transaction reference. Both Razorpay integrations are off by default and
  fail *honestly* (a clear "not configured" or the real gateway error) rather than faking success
  — see `backend/README.md` for how to turn them on with real credentials.
- **Insurance** — data model only (policy records + linkage to child/driver/vehicle exist);
  renewal/expiry-escalation workflows aren't built.
- **Mobile** — the web app wrapped with Capacitor. Android is fully working (a real debug APK
  builds locally via Gradle); iOS is scaffolded but can only be built from Xcode on macOS.

Automated tests exist on both sides and pass: **11 JUnit integration tests** (backend, real HTTP
against a real Postgres instance) covering auth, tenant isolation, the independent-driver claim
flow, geo-based route auto-assignment, and the grievance flow; **26 Vitest/RTL tests** (frontend)
covering stores, shared components, and full page flows against a mocked API.

## Architecture decisions worth knowing about

- **Modular monolith, not microservices** — one deployable, Maven-module-enforced boundaries.
  Splittable later if a specific module (most likely `module-tracking`) needs to scale
  independently.
- **GPS hardware via Traccar** — dedicated in-vehicle trackers speak proprietary protocols;
  Traccar (self-hosted, open source) normalizes ~170 of them and forwards clean JSON to our
  ingestion webhook, rather than us reimplementing device protocols.
- **PostgreSQL + PostGIS** — one relational store, native geospatial queries power geofencing and
  route-zone auto-assignment.
- **Cross-module references are ids, not JPA relationships** — e.g. `Vehicle.insurancePolicyId` is
  a plain `Long`, not a `@ManyToOne` into `module-insurance`'s entity. Keeps modules independently
  extractable into real services later without an entity-graph untangling project.
- **Razorpay over Stripe** — the original plan named Stripe, but the product spec asks for
  UPI/QR specifically; Razorpay is India-native and supports all of it through one Checkout
  integration, where Stripe's India/UPI support is limited. RazorpayX (a separate product) handles
  driver payouts.
- **Payroll disbursement is a real API integration, not a mock** — it makes genuine HTTP calls to
  RazorpayX's live endpoints. It's off by default because it needs a real, KYC'd RazorpayX
  business account, which can't be provisioned from inside a coding session.
