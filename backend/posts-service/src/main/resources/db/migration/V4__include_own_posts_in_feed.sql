-- The feed now includes the current user's own posts as well as posts from
-- everyone else. Keep like totals and "liked by me" semantics unchanged.

CREATE OR REPLACE FUNCTION sp_get_posts_with_likes(p_current_user_id UUID)
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
    GROUP BY p.id
    ORDER BY p.published_at DESC;
END;
$$;
