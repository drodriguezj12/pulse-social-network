# Pulse — Backend (microservicios Spring Boot)

Dos microservicios independientes (build, Docker y tests propios). Java 21,
Spring Boot 3.3, PostgreSQL 16 + Flyway, JWT HS256 compartido por configuración.

| Servicio | Puerto | Responsabilidad | Schema |
|---|---|---|---|
| [`auth-service`](auth-service) | 8081 | Login JWT (POST + GET literal), perfil propio y perfiles de solo lectura | `auth` |
| [`posts-service`](posts-service) | 8082 | Publicaciones, borrado propio, likes (stored procedures) y WebSocket | `posts` |

## Puntos clave

- **Stored procedures PL/pgSQL** (requisito): `sp_register_like` y `sp_remove_like`
  son `PROCEDURE` reales invocadas con `CALL` desde Java (`CallableStatement`,
  datasource con `escapeSyntaxCallMode=callIfNoReturn`); `sp_get_posts_with_likes`
  es `FUNCTION RETURNS TABLE` para leer el feed. Ver
  `posts-service/src/main/resources/db/migration/V2__create_procedures.sql`.
- **Tiempo real**: cada like/unlike difunde `{postId, likeCount}` a `/topic/likes`
  (STOMP sobre `/ws`).
- **Seeder**: al arrancar, auth crea 5 usuarios (BCrypt) y posts una publicación por
  usuario, con UUIDs fijos compartidos por convención (sin llamadas entre servicios).
- **Calidad**: DTOs (records), Bean Validation, `@RestControllerAdvice` con error
  consistente `{timestamp, status, error, message, path}`, logs de auditoría SLF4J
  (login, creación, likes), Actuator (`/actuator/health`, `/actuator/metrics`),
  Swagger UI en **`/docs`** de cada servicio.

## Ejecutar tests

Desde cada servicio (`auth-service/` o `posts-service/`):

```bash
mvn test     # unitarios — no requieren Docker
mvn verify   # unitarios + integración (Testcontainers, requiere Docker)
```

Los tests de integración levantan un PostgreSQL 16 real y cubren: flujo de login,
feed con publicaciones propias incluidas, **ejecución real de las
procedures** (idempotencia verificada contra la BD) y recepción del broadcast en un
cliente STOMP real.

## Build de imágenes

Cada servicio tiene Dockerfile multi-stage (Maven → JRE 21 alpine, usuario no root):

```bash
docker build -t pulse-auth-service ./auth-service
docker build -t pulse-posts-service ./posts-service
```
