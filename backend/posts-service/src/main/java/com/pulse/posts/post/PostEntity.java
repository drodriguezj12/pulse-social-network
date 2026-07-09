package com.pulse.posts.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    private UUID id;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    /**
     * Denormalized from the JWT at creation time so the feed never needs a
     * runtime call to auth-service (microservice autonomy over normalization).
     */
    @Column(name = "author_alias", nullable = false, length = 50)
    private String authorAlias;

    @Column(nullable = false, length = 500)
    private String message;

    /** Publication date is assigned by default when saving, per the spec. */
    @CreationTimestamp
    @Column(name = "published_at", nullable = false, updatable = false)
    private OffsetDateTime publishedAt;

    protected PostEntity() {
        // JPA
    }

    public PostEntity(UUID authorId, String authorAlias, String message) {
        this.id = UUID.randomUUID();
        this.authorId = authorId;
        this.authorAlias = authorAlias;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getAuthorAlias() {
        return authorAlias;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
