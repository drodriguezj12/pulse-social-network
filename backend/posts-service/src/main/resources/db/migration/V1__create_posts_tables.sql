-- Posts and likes, owned exclusively by posts-service (schema "posts").
-- author_id references auth.users only logically: no cross-schema FK, so each
-- service keeps full ownership of its own data (microservice autonomy).
-- author_alias is denormalized from the JWT at creation time to avoid a
-- runtime call to auth-service when rendering the feed.

CREATE TABLE posts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id    UUID         NOT NULL,
    author_alias VARCHAR(50)  NOT NULL,
    message      VARCHAR(500) NOT NULL,
    published_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_published_at ON posts (published_at DESC);
CREATE INDEX idx_posts_author_id ON posts (author_id);

CREATE TABLE likes (
    post_id    UUID        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Composite PK = a user can like a post at most once (DB-level idempotency)
    PRIMARY KEY (post_id, user_id)
);
