package com.pulse.posts.post;

import com.pulse.posts.post.dto.FeedCursor;
import com.pulse.posts.post.dto.PostResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reads the feed through the set-returning PL/pgSQL function
 * sp_get_posts_with_likes (see the db/migration folder): a page of posts,
 * newest first, each with its like count, whether the current user already
 * liked it, and whether it carries an image.
 */
@Repository
public class PostFeedDao {

    // The cursor parameters are explicitly cast: when they are null the driver
    // sends an untyped parameter, and PostgreSQL needs the type to resolve the call.
    private static final String SELECT_PAGE =
            "SELECT * FROM posts.sp_get_posts_with_likes(?, ?, ?::timestamptz, ?::uuid)";

    private static final RowMapper<PostResponse> MAPPER = (rs, rowNum) -> new PostResponse(
            rs.getObject("id", UUID.class),
            rs.getObject("author_id", UUID.class),
            rs.getString("author_alias"),
            rs.getString("message"),
            rs.getObject("published_at", OffsetDateTime.class),
            rs.getLong("like_count"),
            rs.getBoolean("liked_by_me"),
            rs.getBoolean("has_image"));

    private final JdbcTemplate jdbcTemplate;

    public PostFeedDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param cursor position of the last post the client already has, or null for the first page
     * @param limit  how many rows to read; callers ask for one extra row to detect a next page
     */
    public List<PostResponse> findFeedFor(UUID currentUserId, FeedCursor cursor, int limit) {
        return jdbcTemplate.query(SELECT_PAGE, MAPPER,
                currentUserId,
                limit,
                cursor == null ? null : cursor.publishedAt(),
                cursor == null ? null : cursor.id());
    }
}
