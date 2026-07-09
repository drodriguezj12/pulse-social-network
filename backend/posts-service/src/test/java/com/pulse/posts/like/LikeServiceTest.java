package com.pulse.posts.like;

import com.pulse.posts.common.NotFoundException;
import com.pulse.posts.like.dto.LikeResponse;
import com.pulse.posts.post.PostRepository;
import com.pulse.posts.security.AuthenticatedUser;
import com.pulse.posts.ws.LikeBroadcaster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeDao likeDao;

    @Mock
    private LikeBroadcaster likeBroadcaster;

    @InjectMocks
    private LikeService likeService;

    private final UUID postId = UUID.randomUUID();
    private final AuthenticatedUser valentina = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000003"), "valentina", "valen");

    @Test
    @DisplayName("liking runs the stored procedure and broadcasts the new total")
    void likeBroadcastsNewTotal() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeDao.registerLike(postId, valentina.id())).thenReturn(4L);

        LikeResponse response = likeService.like(postId, valentina);

        assertThat(response.likeCount()).isEqualTo(4);
        assertThat(response.likedByMe()).isTrue();
        verify(likeBroadcaster).broadcast(postId, 4L);
    }

    @Test
    @DisplayName("unliking runs the stored procedure and broadcasts the new total")
    void unlikeBroadcastsNewTotal() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeDao.removeLike(postId, valentina.id())).thenReturn(3L);

        LikeResponse response = likeService.unlike(postId, valentina);

        assertThat(response.likeCount()).isEqualTo(3);
        assertThat(response.likedByMe()).isFalse();
        verify(likeBroadcaster).broadcast(postId, 3L);
    }

    @Test
    @DisplayName("liking a missing post fails with 404 and broadcasts nothing")
    void likeMissingPost() {
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThatThrownBy(() -> likeService.like(postId, valentina))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(likeDao, likeBroadcaster);
    }
}
