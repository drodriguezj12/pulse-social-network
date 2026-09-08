# Pulse — Backend (Spring Boot microservices)

Two independent services, each with its own build, Docker image, database schema and
test suite. Java 21, Spring Boot 3.3, PostgreSQL 16 with Flyway, HS256 JWT shared
through configuration.

| Service | Port | Responsibility | Schema |
|---|---|---|---|
| [`auth-service`](auth-service) | 8081 | JWT login, own profile (editable alias + avatar), read-only profiles of other users | `auth` |
| [`posts-service`](posts-service) | 8082 | Posts with optional images, author-only deletion, likes via stored procedures, WebSocket broadcasting | `posts` |

## Highlights

- **PL/pgSQL stored procedures.** `sp_register_like` and `sp_remove_like` are real
  `PROCEDURE`s invoked with `CALL` from Java (`CallableStatement`, datasource with
  `escapeSyntaxCallMode=callIfNoReturn` so the driver emits `CALL` instead of
  `SELECT`); `sp_get_posts_with_likes` is a `FUNCTION RETURNS TABLE` that builds one
  page of the feed with like counts and `liked_by_me` in a single round trip. See
  `posts-service/src/main/resources/db/migration/`.
- **Keyset pagination.** The feed seeks by `(published_at, id)` against a matching
  index instead of using `OFFSET`, so page ten costs the same as page one and
  concurrent inserts never duplicate or skip a post. The cursor is an opaque token.
- **Idempotent likes.** Composite primary key `(post_id, user_id)` plus
  `ON CONFLICT DO NOTHING` inside the procedure: liking twice leaves one row and
  returns the correct total.
- **Real time.** Every like/unlike broadcasts `{postId, likeCount}` to `/topic/likes`,
  new posts go to `/topic/posts` and deletions to `/topic/posts-deleted` (STOMP over
  `/ws`), so open feeds grow, shrink and update on their own.
- **No inter-service calls.** The JWT carries `sub`, `username` and `alias`; posts
  store the author alias denormalized. Renaming re-issues the token, and the client
  then calls `POST /posts/author-alias` so the write that realigns old posts happens
  on a write path rather than on every feed read.
- **Secrets without fallbacks.** `JWT_SECRET` has no default: the services refuse to
  start without it, so no deployment can inherit a value committed to the repository.
- **Schema owned by Flyway.** `ddl-auto: validate` — Hibernate only checks that the
  entities match; migrations are versioned in the repository, one history per service.
- **Production hygiene.** Record DTOs (entities are never exposed), Bean Validation,
  a `@RestControllerAdvice` returning a consistent
  `{timestamp, status, error, message, path}` body, SLF4J audit logs for login, post
  creation, likes and deletions, Actuator (`/actuator/health`, `/actuator/metrics`,
  `/actuator/prometheus`) and Swagger UI at **`/docs`** on each service.

## Running the tests

From either service directory (`auth-service/` or `posts-service/`):

```bash
mvn test     # unit tests — no Docker needed
mvn verify   # + integration tests (Testcontainers, requires a Docker daemon)
```

Integration tests boot the full Spring context against a real PostgreSQL 16 and cover
the parts that mocks would only pretend to verify: the login and profile flows, the
stored procedures executing for real (idempotency checked directly against the table),
like counts accumulating across different users, the complete image lifecycle
(multipart upload, public read, rejected content types), author-only deletion, and a
real STOMP client that waits for the broadcast frames.

H2 was deliberately avoided: it cannot execute PL/pgSQL, so a test suite built on it
would skip the most important behaviour in the service.

## Building the images

Each service has a multi-stage Dockerfile (Maven build → JRE 21 Alpine runtime, non-root
user):

```bash
docker build -t pulse-auth-service ./auth-service
docker build -t pulse-posts-service ./posts-service
```
