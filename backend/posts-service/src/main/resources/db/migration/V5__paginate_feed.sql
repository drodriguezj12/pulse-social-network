-- Keyset (cursor) pagination for the feed.
--
-- OFFSET pagination degrades linearly: the database still walks and discards
-- every skipped row, and rows inserted while the user reads shift the window,
-- so posts get duplicated or skipped. Keyset pagination seeks straight to the
-- cursor position using the sort key, which stays O(log n) at any depth and is
-- stable under concurrent inserts.
--
-- The sort key is (published_at DESC, id DESC): published_at alone is not
-- unique, so the id breaks ties and keeps the cursor deterministic.

CREATE INDEX idx_posts_keyset ON posts (published_at DESC, id DESC);

-- The signature changes, and PostgreSQL cannot alter the return type or the
-- argument list of an existing function in place.
DROP FUNCTION sp_get_posts_with_likes(UUID);

CREATE FUNCTION sp_get_posts_with_likes(
    p_current_user_id     UUID,
    p_limit               INT,
    p_cursor_published_at TIMESTAMPTZ DEFAULT NULL,
    p_cursor_id           UUID DEFAULT NULL)
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
    -- Row-value comparison: "everything strictly older than the cursor".
    WHERE p_cursor_published_at IS NULL
       OR (p.published_at, p.id) < (p_cursor_published_at, p_cursor_id)
    GROUP BY p.id
    ORDER BY p.published_at DESC, p.id DESC
    LIMIT p_limit;
END;
$$;
