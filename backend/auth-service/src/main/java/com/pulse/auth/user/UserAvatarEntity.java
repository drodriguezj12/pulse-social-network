package com.pulse.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_avatars")
public class UserAvatarEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private byte[] image;

    protected UserAvatarEntity() {
        // JPA
    }

    public UserAvatarEntity(UUID userId, String contentType, byte[] image) {
        this.userId = userId;
        this.contentType = contentType;
        this.image = image;
    }

    public void replaceWith(String contentType, byte[] image) {
        this.contentType = contentType;
        this.image = image;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getImage() {
        return image;
    }
}
