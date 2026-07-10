package com.pulse.posts.post;

import com.pulse.posts.common.InvalidImageException;
import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import com.pulse.posts.ws.PostBroadcaster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostFeedDao postFeedDao;

    @Mock
    private PostBroadcaster postBroadcaster;

    @InjectMocks
    private PostService postService;

    private final AuthenticatedUser carlos = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000002"), "carlos", "cgomez");

    @Test
    @DisplayName("creating a post takes the author from the JWT and broadcasts it")
    void createTakesAuthorFromPrincipal() {
        when(postRepository.saveAndFlush(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.create(carlos, new CreatePostRequest("  hello world  "), null);

        assertThat(response.authorId()).isEqualTo(carlos.id());
        assertThat(response.authorAlias()).isEqualTo("cgomez");
        assertThat(response.message()).isEqualTo("hello world");
        assertThat(response.likeCount()).isZero();
        assertThat(response.hasImage()).isFalse();
        verify(postBroadcaster).broadcast(response);
        verifyNoInteractions(postImageRepository);
    }

    @Test
    @DisplayName("creating a post with an image stores the image and flags hasImage")
    void createWithImage() {
        when(postRepository.saveAndFlush(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        var image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});

        PostResponse response = postService.create(carlos, new CreatePostRequest("with pic"), image);

        assertThat(response.hasImage()).isTrue();
        verify(postImageRepository).save(any(PostImageEntity.class));
        verify(postBroadcaster).broadcast(response);
    }

    @Test
    @DisplayName("a non-image attachment is rejected with 400 semantics")
    void createWithWrongImageType() {
        when(postRepository.saveAndFlush(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        var file = new MockMultipartFile("image", "notes.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> postService.create(carlos, new CreatePostRequest("bad"), file))
                .isInstanceOf(InvalidImageException.class);
        verifyNoInteractions(postBroadcaster);
    }

    @Test
    @DisplayName("feed is read through the stored function for the current user")
    void feedDelegatesToDao() {
        List<PostResponse> feed = List.of(new PostResponse(
                UUID.randomUUID(), UUID.randomUUID(), "marilo", "hi",
                OffsetDateTime.now(), 3, true, false));
        when(postFeedDao.findFeedFor(carlos.id())).thenReturn(feed);

        assertThat(postService.getFeed(carlos)).isEqualTo(feed);
        verify(postRepository).updateAuthorAlias(carlos.id(), carlos.alias());
        verify(postFeedDao).findFeedFor(carlos.id());
    }

    @Test
    @DisplayName("deleting a post removes it when the JWT owner is the author")
    void deleteOwnPost() {
        PostEntity post = new PostEntity(carlos.id(), carlos.alias(), "mine");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.delete(carlos, post.getId());

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("deleting another user's post is forbidden")
    void deleteOtherUsersPostRejected() {
        PostEntity post = new PostEntity(UUID.randomUUID(), "marilo", "not mine");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(carlos, post.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Solo puedes eliminar tus propias publicaciones");
    }
}
