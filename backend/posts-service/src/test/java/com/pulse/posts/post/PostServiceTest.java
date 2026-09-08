package com.pulse.posts.post;

import com.pulse.posts.common.InvalidImageException;
import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.FeedCursor;
import com.pulse.posts.post.dto.PostPage;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    private static PostResponse samplePost(int minutesAgo) {
        return new PostResponse(UUID.randomUUID(), UUID.randomUUID(), "marilo", "hi",
                OffsetDateTime.now().minusMinutes(minutesAgo), 3, true, false);
    }

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
        verify(postBroadcaster).broadcastCreated(response);
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
        verify(postBroadcaster).broadcastCreated(response);
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
    @DisplayName("a full page returns a cursor pointing at its last post")
    void feedReturnsCursorWhenMorePostsExist() {
        // The service asks for limit + 1 rows to detect a next page without a count query
        List<PostResponse> rows = IntStream.range(0, 4).mapToObj(PostServiceTest::samplePost).toList();
        when(postFeedDao.findFeedFor(eq(carlos.id()), isNull(), eq(4))).thenReturn(rows);

        PostPage page = postService.getFeed(carlos, null, 3);

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(FeedCursor.decode(page.nextCursor()).id()).isEqualTo(rows.get(2).id());
    }

    @Test
    @DisplayName("the last page returns a null cursor")
    void feedReturnsNoCursorOnLastPage() {
        when(postFeedDao.findFeedFor(eq(carlos.id()), isNull(), eq(4)))
                .thenReturn(List.of(samplePost(1), samplePost(2)));

        PostPage page = postService.getFeed(carlos, null, 3);

        assertThat(page.items()).hasSize(2);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("an empty feed returns no items and no cursor")
    void feedHandlesEmptyResult() {
        when(postFeedDao.findFeedFor(eq(carlos.id()), isNull(), eq(11))).thenReturn(List.of());

        PostPage page = postService.getFeed(carlos, null, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("reading the feed never writes: the alias sync has its own endpoint")
    void feedDoesNotWrite() {
        when(postFeedDao.findFeedFor(eq(carlos.id()), isNull(), eq(11))).thenReturn(List.of());

        postService.getFeed(carlos, null, 10);

        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("syncing the alias updates the author's posts with the value from the token")
    void syncAuthorAlias() {
        when(postRepository.updateAuthorAlias(carlos.id(), carlos.alias())).thenReturn(3);

        assertThat(postService.syncAuthorAlias(carlos)).isEqualTo(3);
        verify(postRepository).updateAuthorAlias(carlos.id(), carlos.alias());
    }

    @Test
    @DisplayName("deleting a post removes it and broadcasts the removal when the caller is the author")
    void deleteOwnPost() {
        PostEntity post = new PostEntity(carlos.id(), carlos.alias(), "mine");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.delete(carlos, post.getId());

        verify(postRepository).delete(post);
        verify(postBroadcaster).broadcastDeleted(post.getId());
    }

    @Test
    @DisplayName("deleting another user's post is forbidden and broadcasts nothing")
    void deleteOtherUsersPostRejected() {
        PostEntity post = new PostEntity(UUID.randomUUID(), "marilo", "not mine");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(carlos, post.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("You can only delete your own posts");
        verifyNoInteractions(postBroadcaster);
    }
}
