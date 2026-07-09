-- Stored routines required by the technical test (minimum 2 PROCEDUREs in PL/pgSQL).
--
-- PostgreSQL distinguishes PROCEDURE (PG11+, invoked with CALL, may have INOUT
-- params) from FUNCTION (invoked in queries, can return result sets). Returning
-- rows from a PROCEDURE is impractical, so:
--   * sp_register_like / sp_remove_like are real PROCEDUREs (CALL + INOUT count),
--   * sp_get_posts_with_likes is a set-returning FUNCTION (the idiomatic way to
--     return rows in PostgreSQL).
-- Table names are schema-qualified because routines execute with the CALLER's
-- search_path, not the one used during this migration.

CREATE OR REPLACE PROCEDURE sp_register_like(
    p_post_id     UUID,
    p_user_id     UUID,
    INOUT o_like_count BIGINT DEFAULT NULL)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM posts.posts WHERE id = p_post_id) THEN
        RAISE EXCEPTION 'Post % not found', p_post_id USING ERRCODE = 'P0002';
    END IF;

    -- Idempotent: the composite PK plus ON CONFLICT makes a second like a no-op.
    INSERT INTO posts.likes (post_id, user_id)
    VALUES (p_post_id, p_user_id)
    ON CONFLICT (post_id, user_id) DO NOTHING;

    SELECT count(*) INTO o_like_count FROM posts.likes WHERE post_id = p_post_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_remove_like(
    p_post_id     UUID,
    p_user_id     UUID,
    INOUT o_like_count BIGINT DEFAULT NULL)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM posts.posts WHERE id = p_post_id) THEN
        RAISE EXCEPTION 'Post % not found', p_post_id USING ERRCODE = 'P0002';
    END IF;

    DELETE FROM posts.likes
    WHERE post_id = p_post_id AND user_id = p_user_id;

    SELECT count(*) INTO o_like_count FROM posts.likes WHERE post_id = p_post_id;
END;
$$;

CREATE OR REPLACE FUNCTION sp_get_posts_with_likes(p_current_user_id UUID)
RETURNS TABLE (
    id           UUID,
    author_id    UUID,
    author_alias VARCHAR(50),
    message      VARCHAR(500),
    published_at TIMESTAMPTZ,
    like_count   BIGINT,
    liked_by_me  BOOLEAN)
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
           count(l.user_id)                                   AS like_count,
           COALESCE(bool_or(l.user_id = p_current_user_id), false) AS liked_by_me
    FROM posts.posts p
    LEFT JOIN posts.likes l ON l.post_id = p.id
    WHERE p.author_id <> p_current_user_id  -- feed shows other users' posts only
    GROUP BY p.id
    ORDER BY p.published_at DESC;
END;
$$;
