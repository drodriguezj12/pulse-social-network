package com.pulse.posts.like;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.UUID;

/**
 * Invokes the PL/pgSQL PROCEDUREs (see V2__create_procedures.sql) through
 * JDBC CallableStatement. The {call ...} escape requires the datasource URL
 * parameter escapeSyntaxCallMode=callIfNoReturn so the PostgreSQL driver
 * emits CALL (procedure) instead of SELECT (function).
 */
@Repository
public class LikeDao {

    private final JdbcTemplate jdbcTemplate;

    public LikeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Idempotent: liking twice keeps a single like. Returns the new total. */
    public long registerLike(UUID postId, UUID userId) {
        return callLikeProcedure("{call posts.sp_register_like(?, ?, ?)}", postId, userId);
    }

    /** Returns the new total after removing the like (no-op if absent). */
    public long removeLike(UUID postId, UUID userId) {
        return callLikeProcedure("{call posts.sp_remove_like(?, ?, ?)}", postId, userId);
    }

    private long callLikeProcedure(String sql, UUID postId, UUID userId) {
        Long total = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement call = connection.prepareCall(sql)) {
                call.setObject(1, postId);
                call.setObject(2, userId);
                call.setNull(3, Types.BIGINT);
                call.registerOutParameter(3, Types.BIGINT);
                call.execute();
                return call.getLong(3);
            }
        });
        if (total == null) {
            throw new IllegalStateException("Stored procedure did not return a like count");
        }
        return total;
    }
}
