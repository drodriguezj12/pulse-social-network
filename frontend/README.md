# Pulse — Frontend (Angular 19)

The Pulse SPA: login, profile, a feed with **real-time likes and posts**, and post
creation with images. Custom "live signal" design — dark glass surfaces over an aurora
backdrop, with a single accent colour reserved for the like system, so the only thing
that visibly moves is what just happened.

> The user interface is written in Spanish; code, comments and docs are in English.

## Stack

- **Angular 19** standalone components, strict TypeScript, lazy routes.
- **NgRx SignalStore** (`@ngrx/signals`) as a **root singleton** (`providedIn: 'root'`):
  - `AuthStore` — user, JWT, session state (persisted in `localStorage`).
  - `PostsStore` — feed, optimistic likes, and incoming WebSocket events.
  - Components read **signals only** (`store.posts()`, `auth.user()`, …), never raw
    observables, so a single broadcast updates every view at once.
- **@stomp/rx-stomp** — subscribes to `/topic/likes` and `/topic/posts`; each frame is
  fed into `PostsStore`, which makes counters pulse and new posts slide in without a
  reload. Reconnects on its own (`reconnectDelay: 3000`).
- Functional HTTP interceptor that attaches the JWT and signs out globally on `401`,
  plus route guards (`authGuard` / `anonymousGuard`).
- nginx in production: serves the SPA and proxies `/auth`, `/users`, `/posts` and `/ws`
  so the browser talks to a single origin.

## Structure

```
src/app/
├── core/          # api services, interceptor, guards, ws.service, toasts, utils
├── stores/        # auth.store.ts · posts.store.ts (SignalStore, root singletons)
├── features/
│   ├── login/     # reactive form + demo-user chips
│   ├── feed/      # community feed, like burst, real-time pulse, author-only delete
│   ├── create-post/  # message + optional image with preview
│   └── profile/   # own profile (editable alias + avatar) and read-only profiles
└── shared/        # avatar component with fallback, toasts
```

## Local development

Requires the microservices running (for example `docker compose up` from the repo
root).

```bash
npm install
npm start          # ng serve with proxy.conf.json pointing at :8081 / :8082
```

Open http://localhost:4200. The dev proxy avoids CORS in development too.

## Unit tests

```bash
npm run test:ci
```

22 Karma/Jasmine specs: stores (feed loading, optimistic likes with rollback on
failure, WebSocket event sequencing, local removal after delete), route guards, the
relative-time pipe and avatar utilities.

Needs a Chromium browser. If Chrome is not on the default path, point `CHROME_BIN` at
your binary (Edge and Brave work).

## Production build

```bash
npm run build      # dist/pulse/browser
```

Or with Docker (multi-stage: Node build → nginx):

```bash
docker build -t pulse-frontend .
```
