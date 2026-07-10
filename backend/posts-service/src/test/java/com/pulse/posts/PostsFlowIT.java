package com.pulse.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.posts.post.PostRepository;
import com.pulse.posts.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PostsFlowIT extends AbstractIntegrationTest {

    private static final UUID MARIANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CARLOS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MARIANA_POST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CARLOS_POST = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenFor(UUID id, String username, String alias) {
        return "Bearer " + jwtService.generateToken(id, username, alias);
    }

    @Test
    @DisplayName("seeder creates one post per demo user")
    void seederRan() {
        assertThat(postRepository.count()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("feed excludes the requesting user's own posts")
    void feedExcludesOwnPosts() throws Exception {
        mockMvc.perform(get("/posts")
                        .header("Authorization", tokenFor(MARIANA_ID, "mariana", "marilo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].authorId", not(MARIANA_ID.toString())))
                .andExpect(jsonPath("$[*].authorAlias", not("marilo")))
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$[0].likeCount").isNumber())
                .andExpect(jsonPath("$[0].likedByMe").isBoolean())
                .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());
    }

    @Test
    @DisplayName("a post is created with server-side publication date and JWT author")
    void createPost() throws Exception {
        mockMvc.perform(post("/posts")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Integration test post\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.authorAlias").value("cgomez"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    @DisplayName("malformed body (invalid UTF-8) returns 400, never 500")
    void malformedBodyRejected() throws Exception {
        byte[] invalidUtf8 = {'{', '"', 'm', 'e', 's', 's', 'a', 'g', 'e', '"', ':', '"', (byte) 0xF3, 'n', '"', '}'};
        mockMvc.perform(post("/posts")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez"))
                        .contentType(APPLICATION_JSON)
                        .content(invalidUtf8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("UTF-8 accents and emoji are stored and returned intact")
    void utf8ContentSupported() throws Exception {
        mockMvc.perform(post("/posts")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Acción y emoción ✨\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Acción y emoción ✨"));
    }

    @Test
    @DisplayName("blank message fails Bean Validation with 400")
    void blankMessageRejected() throws Exception {
        mockMvc.perform(post("/posts")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("liking twice is idempotent: sp_register_like keeps a single like")
    void likeIsIdempotent() throws Exception {
        String carlos = tokenFor(CARLOS_ID, "carlos", "cgomez");

        mockMvc.perform(post("/posts/" + MARIANA_POST + "/likes").header("Authorization", carlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        mockMvc.perform(post("/posts/" + MARIANA_POST + "/likes").header("Authorization", carlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        Integer stored = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM posts.likes WHERE post_id = ? AND user_id = ?",
                Integer.class, MARIANA_POST, CARLOS_ID);
        assertThat(stored).isEqualTo(1);

        // liked post is flagged in the feed read through the stored function
        mockMvc.perform(get("/posts").header("Authorization", carlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + MARIANA_POST + "')].likedByMe").value(true));

        // cleanup so other tests see a deterministic state
        mockMvc.perform(delete("/posts/" + MARIANA_POST + "/likes").header("Authorization", carlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    @Test
    @DisplayName("unlike removes the like through sp_remove_like")
    void unlikeRemovesLike() throws Exception {
        String mariana = tokenFor(MARIANA_ID, "mariana", "marilo");

        mockMvc.perform(post("/posts/" + CARLOS_POST + "/likes").header("Authorization", mariana))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/posts/" + CARLOS_POST + "/likes").header("Authorization", mariana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0));

        Integer stored = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM posts.likes WHERE post_id = ? AND user_id = ?",
                Integer.class, CARLOS_POST, MARIANA_ID);
        assertThat(stored).isZero();
    }

    @Test
    @DisplayName("likes from DIFFERENT users accumulate: 1, then 2")
    void likeCountAccumulatesAcrossUsers() throws Exception {
        // fresh post so the assertion is deterministic regardless of test order
        MvcResult created = mockMvc.perform(post("/posts")
                        .header("Authorization", tokenFor(MARIANA_ID, "mariana", "marilo"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"count my likes\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String postId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/posts/" + postId + "/likes")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        UUID valentinaId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        mockMvc.perform(post("/posts/" + postId + "/likes")
                        .header("Authorization", tokenFor(valentinaId, "valentina", "valen")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2));

        Integer stored = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM posts.likes WHERE post_id = ?::uuid", Integer.class, postId);
        assertThat(stored).isEqualTo(2);
    }

    @Test
    @DisplayName("a post with an image stores it, serves it publicly and flags hasImage in the feed")
    void imageLifecycle() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 9, 9};
        MvcResult created = mockMvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("image", "pic.png", "image/png", png))
                        .param("message", "look at this photo")
                        .header("Authorization", tokenFor(MARIANA_ID, "mariana", "marilo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasImage").value(true))
                .andReturn();
        String postId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        // image readable WITHOUT a token (img tags cannot send Authorization)
        mockMvc.perform(get("/posts/" + postId + "/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        // carlos sees the post flagged in his feed
        mockMvc.perform(get("/posts").header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + postId + "')].hasImage").value(true));
    }

    @Test
    @DisplayName("multipart post without image works; wrong attachment type returns 400")
    void multipartValidation() throws Exception {
        mockMvc.perform(multipart("/posts")
                        .param("message", "text only via multipart")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasImage").value(false));

        mockMvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("image", "notes.txt", "text/plain", "x".getBytes()))
                        .param("message", "bad attachment")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("liking a nonexistent post returns a consistent 404 body")
    void likeMissingPost() throws Exception {
        mockMvc.perform(post("/posts/" + UUID.randomUUID() + "/likes")
                        .header("Authorization", tokenFor(CARLOS_ID, "carlos", "cgomez")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Post not found"));
    }

    @Test
    @DisplayName("endpoints without a token return 401")
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/posts")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/posts").contentType(APPLICATION_JSON).content("{\"message\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/posts/" + MARIANA_POST + "/likes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger UI is reachable at /docs and health at /actuator/health")
    void docsAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
