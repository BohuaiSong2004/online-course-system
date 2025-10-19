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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests against a real MySQL (CI starts one as a service container; locally run
 * `docker compose up -d mysql` and export DB_URL). Uses the demo seed data.
 */
@SpringBootTest(properties = {"ocs.rate-limit.enabled=false", "ocs.jwt.reuse-grace-seconds=0"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    // ---------- helpers ----------

    private JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header("Authorization", "Bearer " + token);
    }

    private ResultActions send(MockHttpServletRequestBuilder b, String token, Object payload) throws Exception {
        b = as(b, token);
        if (payload != null) {
            b = b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload));
        }
        return mvc.perform(b);
    }

    private String registerLearner() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        JsonNode res = body(send(post("/api/auth/register"), null,
                java.util.Map.of("email", email, "password", "password123", "displayName", "IT Learner"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly"))));
        assertThat(res.at("/user/role").asText()).isEqualTo("LEARNER");
        return res.get("token").asText();
    }

    private String login(String email) throws Exception {
        return body(send(post("/api/auth/login"), null, java.util.Map.of("email", email, "password", "password123"))
                .andExpect(status().isOk())).get("token").asText();
    }

    // ---------- tests ----------

    @Test
    void catalogIsPublicAndPaginated() throws Exception {
        send(get("/api/courses?size=2&page=1"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.items[0].instructorName").isNotEmpty());
        send(get("/api/courses?q=SQL"), null, null)
                .andExpect(jsonPath("$.items[0].slug").value("sql-for-analysts"));
        send(get("/api/courses/kubernetes-basics-draft"), null, null).andExpect(status().isNotFound());
        send(get("/api/courses/does-not-exist"), null, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void learnerEnrollsReadsAndCompletesACourse() throws Exception {
        String token = registerLearner();
        JsonNode detail = body(send(get("/api/courses/sql-for-analysts"), token, null).andExpect(status().isOk()));
        long courseId = detail.at("/course/id").asLong();
        assertThat(detail.at("/viewer/signedIn").asBoolean()).isTrue();
        assertThat(detail.at("/viewer/enrolled").asBoolean()).isFalse();
        JsonNode chapters = detail.get("chapters");
        long preview = chapters.get(0).get("id").asLong();
        long locked = chapters.get(1).get("id").asLong();

        send(get("/api/chapters/" + preview), null, null).andExpect(status().isOk());
        send(get("/api/chapters/" + locked), null, null).andExpect(status().isUnauthorized());
        send(get("/api/chapters/" + locked), token, null).andExpect(status().isForbidden());
        send(post("/api/courses/" + courseId + "/enroll"), null, null).andExpect(status().isUnauthorized());

        send(post("/api/courses/" + courseId + "/enroll"), token, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        send(post("/api/courses/" + courseId + "/enroll"), token, null).andExpect(status().isOk()); // idempotent
        send(get("/api/chapters/" + locked), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapter.content").isNotEmpty())
                .andExpect(jsonPath("$.prevChapterId").value(preview));

        JsonNode last = null;
        for (JsonNode ch : chapters) {
            long id = ch.get("id").asLong();
            if (ch.get("quizQuestionCount").asInt() > 0) {
                // quiz chapters can't be ticked off; they complete by passing (seed answer key: B, A)
                send(post("/api/chapters/" + id + "/complete"), token, null).andExpect(status().isBadRequest());
                JsonNode quiz = body(send(get("/api/chapters/" + id), token, null)).get("quiz");
                java.util.Map<String, Long> answers = java.util.Map.of(
                        quiz.get(0).get("id").asText(), quiz.get(0).at("/options/1/id").asLong(),
                        quiz.get(1).get("id").asText(), quiz.get(1).at("/options/0/id").asLong());
                last = body(send(post("/api/chapters/" + id + "/quiz/attempts"), token,
                        java.util.Map.of("answers", answers)).andExpect(status().isOk())).get("progress");
            } else {
                last = body(send(post("/api/chapters/" + id + "/complete"), token, null).andExpect(status().isOk()));
            }
        }
        assertThat(last.get("certificateCode").asText()).hasSize(12);
        assertThat(last.get("courseCompleted").asBoolean()).isTrue();
        assertThat(last.get("completedChapters").asInt()).isEqualTo(chapters.size());

        send(get("/api/me/enrollments"), token, null)
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].completedChapters").value(chapters.size()));
        send(get("/api/me/activity?limit=5"), token, null)
                .andExpect(jsonPath("$[0].action").value("COMPLETE_COURSE"))
                .andExpect(jsonPath("$[0].createdAt").value(org.hamcrest.Matchers.endsWith("Z"))); // UTC instant

        send(delete("/api/courses/" + courseId + "/enroll"), token, null).andExpect(status().isNoContent());
        send(get("/api/me/enrollments"), token, null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sessionEndpointNeverFails() throws Exception {
        send(get("/api/auth/session"), null, null).andExpect(status().isOk()).andExpect(jsonPath("$.user").isEmpty());
        send(get("/api/auth/session"), registerLearner(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("LEARNER"));
    }

    @Test
    void learnersCannotAuthor() throws Exception {
        String token = registerLearner();
        send(get("/api/studio/courses"), token, null).andExpect(status().isForbidden());
        send(get("/api/studio/courses"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void instructorAuthorsAndPublishesACourse() throws Exception {
        String token = login("instructor@demo.dev");
        String title = "IT Course " + UUID.randomUUID().toString().substring(0, 8);
        java.util.Map<String, Object> course = java.util.Map.of(
                "title", title, "summary", "made by a test", "description", "desc",
                "category", "Testing", "level", "BEGINNER");
        JsonNode created = body(send(post("/api/studio/courses"), token, course).andExpect(status().isCreated()));
        long id = created.get("id").asLong();
        String slug = created.get("slug").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        send(post("/api/studio/courses/" + id + "/publish"), token, null).andExpect(status().isBadRequest());

        java.util.Map<String, Object> ch = java.util.Map.of("title", "Intro", "content", "# hi", "durationMin", 5,
                "preview", true);
        JsonNode c1 = body(send(post("/api/studio/courses/" + id + "/chapters"), token, ch)
                .andExpect(status().isCreated()));
        assertThat(c1.get("position").asInt()).isEqualTo(1);

        send(post("/api/studio/courses/" + id + "/publish"), token, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Catalog read (cached) then a chapter write must invalidate the cached chapter count.
        send(get("/api/courses/" + slug), null, null).andExpect(jsonPath("$.course.chapterCount").value(1));
        JsonNode c2 = body(send(post("/api/studio/courses/" + id + "/chapters"), token,
                java.util.Map.of("title", "Second", "content", "two", "durationMin", 7)).andExpect(status().isCreated()));
        send(get("/api/courses/" + slug), null, null)
                .andExpect(jsonPath("$.course.chapterCount").value(2))
                .andExpect(jsonPath("$.course.totalMinutes").value(12));

        send(put("/api/studio/courses/" + id + "/chapters/order"), token,
                java.util.Map.of("chapterIds", java.util.List.of(c2.get("id").asLong(), c1.get("id").asLong())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Second"));

        // Another instructor may not edit it.
        String other = login("mentor@demo.dev");
        send(put("/api/studio/courses/" + id), other, course).andExpect(status().isForbidden());

        send(get("/api/courses?q=" + title.substring(10)), null, null)
                .andExpect(jsonPath("$.items[0].slug").value(slug));

        send(delete("/api/studio/courses/" + id), token, null).andExpect(status().isNoContent());
        send(get("/api/courses/" + slug), null, null).andExpect(status().isNotFound());
    }

    @Test
    void refreshTokensRotateAndReuseRevokesAllSessions() throws Exception {
        String email = "rt-" + UUID.randomUUID() + "@example.com";
        JsonNode reg = body(send(post("/api/auth/register"), null,
                java.util.Map.of("email", email, "password", "password123", "displayName", "Rotating"))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.startsWith("ocs_refresh=")))));
        String r1 = reg.get("refreshToken").asText();

        JsonNode rotated = body(send(post("/api/auth/refresh"), null, java.util.Map.of("refreshToken", r1))
                .andExpect(status().isOk()));
        String r2 = rotated.get("refreshToken").asText();
        assertThat(r2).isNotEqualTo(r1);
        send(get("/api/auth/me"), rotated.get("token").asText(), null).andExpect(status().isOk());

        // Replaying the old token = theft: rejected, and the newer token is revoked too.
        send(post("/api/auth/refresh"), null, java.util.Map.of("refreshToken", r1)).andExpect(status().isUnauthorized());
        send(post("/api/auth/refresh"), null, java.util.Map.of("refreshToken", r2)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode s1 = body(send(post("/api/auth/login"), null,
                java.util.Map.of("email", "learner@demo.dev", "password", "password123")).andExpect(status().isOk()));
        String rt = s1.get("refreshToken").asText();
        send(post("/api/auth/logout"), null, java.util.Map.of("refreshToken", rt)).andExpect(status().isNoContent());
        send(post("/api/auth/refresh"), null, java.util.Map.of("refreshToken", rt)).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeSignsOutOtherSessions() throws Exception {
        String email = "pw-" + UUID.randomUUID() + "@example.com";
        JsonNode reg = body(send(post("/api/auth/register"), null,
                java.util.Map.of("email", email, "password", "password123", "displayName", "Pw"))
                .andExpect(status().isCreated()));
        String token = reg.get("token").asText();
        String oldRefresh = reg.get("refreshToken").asText();

        send(put("/api/me/password"), token, java.util.Map.of("currentPassword", "wrong-one", "newPassword", "newpassword1"))
                .andExpect(status().isBadRequest());
        JsonNode fresh = body(send(put("/api/me/password"), token,
                java.util.Map.of("currentPassword", "password123", "newPassword", "newpassword1"))
                .andExpect(status().isOk()));
        assertThat(fresh.get("refreshToken").asText()).isNotEqualTo(oldRefresh);

        send(post("/api/auth/refresh"), null, java.util.Map.of("refreshToken", oldRefresh)).andExpect(status().isUnauthorized());
        send(post("/api/auth/login"), null, java.util.Map.of("email", email, "password", "password123"))
                .andExpect(status().isUnauthorized());
        send(post("/api/auth/login"), null, java.util.Map.of("email", email, "password", "newpassword1"))
                .andExpect(status().isOk());

        send(put("/api/me/profile"), fresh.get("token").asText(), java.util.Map.of("displayName", "Renamed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Renamed"));
    }

    @Test
    void validationErrorsAreReported() throws Exception {
        send(post("/api/auth/register"), null, java.util.Map.of("email", "nope", "password", "x", "displayName", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
        send(post("/api/auth/login"), null, java.util.Map.of("email", "learner@demo.dev", "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
    }
}
