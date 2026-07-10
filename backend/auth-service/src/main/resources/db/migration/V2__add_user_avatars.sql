-- Profile pictures, stored in a separate table so loading a user (e.g. during
-- login) never drags megabytes of image bytes into memory.

CREATE TABLE user_avatars (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    image        BYTEA       NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
