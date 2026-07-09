package com.pulse.posts.post;

import com.pulse.posts.post.dto.PostResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reads the feed through the set-returning PL/pgSQL function
 * sp_get_posts_with_likes (see V2__create_procedures.sql): posts by OTHER
 * users, newest first, each with its like count and whether the current
 * user already liked it.
 */
@Repository
public class PostFeedDao {

    private static final RowMapper<PostResponse> MAPPER = (rs, rowNum) -> new PostResponse(
            rs.getObject("id", UUID.class),
            rs.getObject("author_id", UUID.class),
            rs.getString("author_alias"),
            rs.getString("message"),
            rs.getObject("published_at", OffsetDateTime.class),
            rs.getLong("like_count"),
            rs.getBoolean("liked_by_me"));

    private final JdbcTemplate jdbcTemplate;

    public PostFeedDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostResponse> findFeedFor(UUID currentUserId) {
        return jdbcTemplate.query("SELECT * FROM posts.sp_get_posts_with_likes(?)", MAPPER, currentUserId);
    }
}
