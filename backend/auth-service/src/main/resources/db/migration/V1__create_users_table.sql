-- Users table, owned exclusively by auth-service (schema "auth").
-- Flyway runs with default-schema=auth, so unqualified names land there.

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    birth_date    DATE         NOT NULL,
    alias         VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
