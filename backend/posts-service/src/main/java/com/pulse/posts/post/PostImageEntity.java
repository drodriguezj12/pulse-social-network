package com.pulse.posts.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "post_images")
public class PostImageEntity {

    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private byte[] image;

    protected PostImageEntity() {
        // JPA
    }

    public PostImageEntity(UUID postId, String contentType, byte[] image) {
        this.postId = postId;
        this.contentType = contentType;
        this.image = image;
    }

    public UUID getPostId() {
        return postId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getImage() {
        return image;
    }
}
