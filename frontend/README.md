# ProjectX Frontend

React 19 + TypeScript + Vite, Tailwind CSS v4, TanStack Query, Zustand, react-hook-form + zod,
react-leaflet, STOMP-over-SockJS for live vehicle tracking.

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
  app/            providers (TanStack Query)
  components/
    ui/           hand-rolled Tailwind primitives (Button, Card, Input, Badge)
    layout/       AppShell (role-based nav), ProtectedRoute (role guard)
  features/       one folder per role: platform-admin, school-admin, driver, parent, auth, public
  hooks/          useLiveVehicleLocation - the STOMP subscription behind the live map
  lib/
    apiClient.ts  axios instance with JWT attach + refresh-token retry
    wsClient.ts   STOMP client factory (auth via CONNECT frame header, not the WS handshake)
    authStore.ts  zustand store, persisted to localStorage
    api/          one file per backend module, typed request/response functions
  types/          TS types mirroring the backend's DTOs
```

## What's wired up vs. what's still a stub

Functional against the real backend: login/JWT refresh, platform-admin school onboarding,
school-admin vehicle/driver/route/trip management, driver trip start/complete + child
board/drop marking, parent live tracking map + home-arrival confirmation + notification history.

Not built yet (matches the backend's phase-1 scope): insurance policy management UI, Stripe
billing UI, driver payroll UI. See the root README for the overall phasing.
