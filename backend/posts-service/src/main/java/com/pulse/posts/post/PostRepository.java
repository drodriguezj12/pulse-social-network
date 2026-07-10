package com.pulse.posts.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    @Modifying
    @Query("update PostEntity p set p.authorAlias = :alias where p.authorId = :authorId and p.authorAlias <> :alias")
    int updateAuthorAlias(@Param("authorId") UUID authorId, @Param("alias") String alias);
}
