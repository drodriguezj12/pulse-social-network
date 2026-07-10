-- One optional image per post, in its own table so listing the feed never
-- loads image bytes (they are served on demand by GET /posts/{id}/image).

CREATE TABLE post_images (
    post_id      UUID PRIMARY KEY REFERENCES posts (id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    image        BYTEA       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The feed function gains a has_image column. PostgreSQL cannot change the
-- return signature with CREATE OR REPLACE, so drop and recreate.
DROP FUNCTION sp_get_posts_with_likes(UUID);

CREATE FUNCTION sp_get_posts_with_likes(p_current_user_id UUID)
RETURNS TABLE (
    id           UUID,
    author_id    UUID,
    author_alias VARCHAR(50),
    message      VARCHAR(500),
    published_at TIMESTAMPTZ,
    like_count   BIGINT,
    liked_by_me  BOOLEAN,
    has_image    BOOLEAN)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT p.id,
           p.author_id,
           p.author_alias,
           p.message,
           p.published_at,
           count(l.user_id)                                        AS like_count,
           COALESCE(bool_or(l.user_id = p_current_user_id), false) AS liked_by_me,
           EXISTS (SELECT 1 FROM posts.post_images i WHERE i.post_id = p.id) AS has_image
    FROM posts.posts p
    LEFT JOIN posts.likes l ON l.post_id = p.id
    WHERE p.author_id <> p_current_user_id  -- feed shows other users' posts only
    GROUP BY p.id
    ORDER BY p.published_at DESC;
END;
$$;
