package com.pulse.posts.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostImageRepository extends JpaRepository<PostImageEntity, UUID> {
}
