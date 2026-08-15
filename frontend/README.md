# Sunju (ProjectX) Frontend

React 19 + TypeScript + Vite, Tailwind CSS v4, TanStack Query, Zustand, react-hook-form + zod,
react-leaflet (incl. a click-to-drop-a-pin location picker), STOMP-over-SockJS for live vehicle
tracking and live grievance-status push, Razorpay Checkout (loaded from their CDN, not an npm
package) for payments. Also ships as an Android app via Capacitor - see `MOBILE.md`.

The public marketing pages use real photos (sourced from Pexels, free license, courtesy-credited
in the footer) of an actual Indian school bus and schoolchildren, rather than generic stock
imagery; nothing inside the authenticated product claims to show a photo of an actual enrolled
child, driver, or vehicle.

## Setup

```sh
npm install
cp .env.example .env   # defaults are fine for local dev against the backend on :8080
npm run dev
```

Opens on http://localhost:5173. `vite.config.ts` proxies `/api` and `/ws` to
`http://localhost:8080`, so the backend (see `../backend/README.md`) needs to be running first.

## Structure

```
src/
  app/                providers (TanStack Query)
  components/
    ui/               hand-rolled Tailwind primitives (Button, Card, Input, Badge, LocationPicker)
    illustrations/     hand-drawn SVG per-role icons (used on the home page's benefit cards)
    grievance/          shared GrievanceStatusBadge
    layout/             AppShell (role-based nav), ProtectedRoute (role guard), PublicHeader/Footer
  features/            one folder per role: platform-admin, school-admin, driver, parent, auth, public
  hooks/
    useLiveVehicleLocation.ts   STOMP subscription behind the live tracking map
    useLiveTopic.ts              generic "refetch on any STOMP message" hook (grievances use this)
  lib/
    apiClient.ts        axios instance with JWT attach + refresh-token retry
    wsClient.ts          STOMP client factory (auth via CONNECT frame header, not the WS handshake)
    razorpayCheckout.ts  loads checkout.razorpay.com's script, opens the payment modal
    authStore.ts         zustand store, persisted to localStorage
    toastStore.ts        mutation feedback toasts
    errorMessage.ts       pulls the backend's ApiError.message out of a failed axios call
    api/                 one file per backend module, typed request/response functions
  types/                TS types mirroring the backend's DTOs
  test/setup.ts          Vitest + Testing Library setup
android/, ios/          Capacitor native project shells - see MOBILE.md
```

## What's wired up

Everything below is functional against the real backend and has been exercised in a real browser
(Playwright), not just written and assumed to work:

- **Auth** — login/JWT refresh, invite-accept flow, role-based routing for all four roles.
- **Branding & marketing** — a proper multi-page marketing site (`/`, `/about`, `/vision`,
  `/benefits`, sharing one header/footer/CTA via `PublicLayout`), not one long single-page
  scroll. Real, freely-licensed photos (credited in the footer) plus hand-drawn SVG icons for
  the compact per-role benefit cards; the home page's hero is a self-drawn live-tracking mockup
  rather than a stock photo standing in for the feature.
- **Dashboards** — a real landing dashboard for every role, not just a stat-card afterthought: parent
  (children summary, fee-due/overdue banner, open grievances, recent notifications, quick actions);
  school admin (fleet/roster stats, today's trips, open grievances, this month's revenue, payroll
  cycle status, independent-driver-pool size); platform admin (cross-school stats, open grievances,
  pending late-fee waivers, independent-driver-pool size); driver (today's trips, last payroll
  status, bank-details-on-file check, open grievances).
- **Header** — a live clock (date + seconds-resolution time) on every authenticated page, and a
  one-click "Track bus" shortcut in the header when logged in as a parent, from any page.
- **Insurance** — school admin can add a policy (provider, policy number, coverage, start/end date)
  for a child, driver, or vehicle in one step (creates the policy and links it), see all policies
  for the school with an expiring-within-30-days warning, and see an Insured/Not insured badge on
  the children/drivers/vehicles rosters and the parent's own children page.
- **Full CRUD** — platform admin manages schools (create/edit/suspend); school admin manages
  vehicles/drivers/children/parents (create/edit/deactivate) and configures route pickup-radius
  zones; parents edit their own profile and their children's details.
- **Independent drivers** — a public self-registration page (driver + their own vehicle details);
  school admins browse and "hire" (claim) from the pool of unclaimed drivers.
- **Location picker** — every place a lat/lng pair used to need typing by hand (school location,
  parent home address, route stops) is now click-to-drop-a-pin on a map instead.
- **Live tracking** — parent-facing map with a live-updating vehicle position and a
  confirm-child-reached-home-safely button.
- **Grievances** — raise (parent/driver/school-admin) and resolve (school/platform-admin) flows for
  all four roles, with the list updating live over STOMP when someone else changes something -
  no manual refresh needed.
- **Payments** — parent: pay a fee via Razorpay Checkout (UPI/QR/card/netbanking/wallet all come
  from that one widget), prepay several months for a discount, view payment history/receipts,
  request a late-fee waiver with a reason. School admin: set the monthly fee, see invoices/payments
  received, set a driver's incentive before payroll runs. Platform admin: review waiver requests,
  manually trigger the three billing jobs. Driver: enter bank details, view salary history
  including the transaction reference once actually disbursed.
- **Trips** — driver start/complete a trip + mark each child boarded/dropped.

## Testing

```sh
npm test        # Vitest + React Testing Library - stores, shared components, full page flows
```

26 tests: pure-logic unit tests (`cn`, `authStore`'s JWT-claim decoding, `toastStore`), component
tests (`Button`, `GrievanceStatusBadge`), and full page-level tests with the API layer mocked
(`LoginPage`'s success/failure/validation paths, `DriversPage`'s roster + independent-driver-pool +
claim flow).

## Mobile

The same app, wrapped with [Capacitor](https://capacitorjs.com/) rather than a second codebase -
see `MOBILE.md` for the full build steps. Short version: Android is fully working (a real debug
APK builds locally via Gradle); iOS is scaffolded (`ios/`) but can only actually be built from
Xcode on macOS, which isn't available in every environment this runs in.
