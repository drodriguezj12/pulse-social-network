package com.pulse.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.auth.security.JwtService;
import com.pulse.auth.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("seeder creates the five demo users on startup")
    void seederRan() {
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(userRepository.findByUsername("mariana")).isPresent();
    }

    @Test
    @DisplayName("POST /auth/login returns a JWT for valid credentials")
    void postLoginHappyPath() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"mariana\",\"password\":\"Pulse2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("mariana"))
                .andExpect(jsonPath("$.user.alias").value("marilo"));
    }

    @Test
    @DisplayName("GET /auth/login works with X-Username/X-Password headers (spec compliance)")
    void getLoginWithHeaders() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .header("X-Username", "carlos")
                        .header("X-Password", "Pulse2026!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/login works with query parameters (spec compliance)")
    void getLoginWithQueryParams() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .param("username", "valentina")
                        .param("password", "Pulse2026!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/login without credentials returns 400")
    void getLoginWithoutCredentials() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid credentials return a consistent 401 error body")
    void invalidCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"mariana\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("blank credentials fail Bean Validation with 400")
    void blankCredentialsRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /users/me returns the profile of the token owner")
    void profileWithToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"daniela\",\"password\":\"Pulse2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Daniela"))
                .andExpect(jsonPath("$.lastName").value("Mora"))
                .andExpect(jsonPath("$.birthDate").value("1996-09-30"))
                .andExpect(jsonPath("$.alias").value("danim"));
    }

    @Test
    @DisplayName("GET /users/me without a token returns 401")
    void profileWithoutToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/{id} lets an authenticated user view another user's profile")
    void publicProfileWithToken() throws Exception {
        String token = loginAndGetToken("carlos");

        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mariana"))
                .andExpect(jsonPath("$.firstName").value("Mariana"))
                .andExpect(jsonPath("$.lastName").value("López"))
                .andExpect(jsonPath("$.alias").value("marilo"));
    }

    @Test
    @DisplayName("GET /users/{id} without a token returns 401")
    void publicProfileWithoutTokenRejected() throws Exception {
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /users/me updates alias but never username or real names")
    void updateProfile() throws Exception {
        String token = loginAndGetToken("andres");

        MvcResult updated = mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alias\":\"andres_live\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.firstName").value("Andrés"))
                .andExpect(jsonPath("$.user.lastName").value("Torres"))
                .andExpect(jsonPath("$.user.username").value("andres"))
                .andExpect(jsonPath("$.user.alias").value("andres_live"))
                .andReturn();

        String newToken = objectMapper.readTree(updated.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(jwtService.parse(newToken).alias()).isEqualTo("andres_live");
    }

    @Test
    @DisplayName("an alias another user already holds is rejected with 409")
    void aliasMustBeUnique() throws Exception {
        String token = loginAndGetToken("valentina");

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alias\":\"MARILO\"}"))   // case-insensitive clash with mariana
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That alias is already taken"));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.alias").value("valen"));
    }

    @Test
    @DisplayName("an alias with unsupported characters is rejected with 400")
    void aliasFormatIsValidated() throws Exception {
        String token = loginAndGetToken("valentina");

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alias\":\"not valid!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("avatar upload, public read and type validation work end to end")
    void avatarLifecycle() throws Exception {
        String token = loginAndGetToken("valentina");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3};

        // before any upload the endpoint still answers, with a generated disc
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000003/avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));

        // an unknown user is the only 404 case
        mockMvc.perform(get("/users/" + UUID.randomUUID() + "/avatar"))
                .andExpect(status().isNotFound());

        mockMvc.perform(multipart("/users/me/avatar")
                        .file(new MockMultipartFile("image", "me.png", "image/png", png))
                        .header("Authorization", "Bearer " + token)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvatar").value(true));

        // public read without any token
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000003/avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        // wrong content type rejected
        mockMvc.perform(multipart("/users/me/avatar")
                        .file(new MockMultipartFile("image", "notes.txt", "text/plain", "x".getBytes()))
                        .header("Authorization", "Bearer " + token)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());
    }

    private String loginAndGetToken(String username) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Pulse2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Swagger UI is reachable at /docs and health at /actuator/health")
    void docsAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
