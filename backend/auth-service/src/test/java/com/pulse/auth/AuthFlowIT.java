package com.pulse.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.auth.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("Swagger UI is reachable at /docs and health at /actuator/health")
    void docsAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
