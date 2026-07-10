# Pulse — Frontend (Angular 19)

SPA de la red social Pulse: login, perfil, feed con **likes en tiempo real** y
creación de publicaciones. Diseño propio "señal en vivo": superficies oscuras
neutras y un solo color de acento (rosa) reservado para el sistema de likes.

## Stack

- **Angular 19** standalone components, TypeScript estricto, rutas lazy.
- **NgRx SignalStore** (`@ngrx/signals`) como **singleton** (`providedIn: 'root'`):
  - `AuthStore` — usuario, token JWT, estado de sesión (persistida en localStorage).
  - `PostsStore` — feed, likes optimistas y eventos del WebSocket.
  - Los componentes consumen **solo signals** (`store.posts()`, `auth.user()`, …).
- **@stomp/rx-stomp** — suscripción a `/topic/likes`; cada broadcast entra al
  `PostsStore` y el contador pulsa en pantalla sin recargar.
- Interceptor HTTP que adjunta el JWT + guard de rutas (`authGuard`).
- nginx en producción: sirve la SPA y hace proxy de `/auth`, `/users`, `/posts` y `/ws`.

## Estructura

```
src/app/
├── core/          # api services, interceptor, guards, ws.service, toasts, utils
├── stores/        # auth.store.ts · posts.store.ts (SignalStore, root singletons)
├── features/
│   ├── login/     # formulario reactivo + chips de usuarios demo
│   ├── feed/      # publicaciones de otros, like con burst + pulse en tiempo real
│   ├── create-post/
│   └── profile/
└── shared/        # componente de toasts
```

## Desarrollo local

Requiere los microservicios corriendo (por ejemplo `docker compose up` desde la raíz).

```bash
npm install
npm start          # ng serve con proxy (proxy.conf.json) hacia :8081/:8082
```

Abre http://localhost:4200. El proxy evita CORS también en desarrollo.

## Build de producción

```bash
npm run build      # dist/pulse/browser
```

O con Docker (multi-stage: build Node → nginx):

```bash
docker build -t pulse-frontend .
```
