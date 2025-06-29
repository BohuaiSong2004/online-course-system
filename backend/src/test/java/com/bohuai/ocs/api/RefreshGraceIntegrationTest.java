package com.bohuai.ocs.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With the default grace window, two near-simultaneous refreshes with one token both succeed. */
@SpringBootTest(properties = "ocs.rate-limit.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class RefreshGraceIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void concurrentRefreshWithinGraceIsNotTreatedAsTheft() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"learner@demo.dev\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String rt = json.readTree(login).get("refreshToken").asText();
        String body = json.writeValueAsString(Map.of("refreshToken", rt));

        JsonNode first = json.readTree(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        // the first branch keeps working: no global revocation happened
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", first.get("refreshToken").asText()))))
                .andExpect(status().isOk());
    }
}
