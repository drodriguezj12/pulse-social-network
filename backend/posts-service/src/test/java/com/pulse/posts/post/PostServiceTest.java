package com.pulse.posts.post;

import com.pulse.posts.post.dto.CreatePostRequest;
import com.pulse.posts.post.dto.PostResponse;
import com.pulse.posts.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostFeedDao postFeedDao;

    @InjectMocks
    private PostService postService;

    private final AuthenticatedUser carlos = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000002"), "carlos", "cgomez");

    @Test
    @DisplayName("creating a post takes author id and alias from the JWT principal")
    void createTakesAuthorFromPrincipal() {
        when(postRepository.saveAndFlush(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.create(carlos, new CreatePostRequest("  hello world  "));

        assertThat(response.authorId()).isEqualTo(carlos.id());
        assertThat(response.authorAlias()).isEqualTo("cgomez");
        assertThat(response.message()).isEqualTo("hello world");
        assertThat(response.likeCount()).isZero();
        assertThat(response.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("feed is read through the stored function for the current user")
    void feedDelegatesToDao() {
        List<PostResponse> feed = List.of(new PostResponse(
                UUID.randomUUID(), UUID.randomUUID(), "marilo", "hi",
                OffsetDateTime.now(), 3, true));
        when(postFeedDao.findFeedFor(carlos.id())).thenReturn(feed);

        assertThat(postService.getFeed(carlos)).isEqualTo(feed);
        verify(postFeedDao).findFeedFor(carlos.id());
    }
}
