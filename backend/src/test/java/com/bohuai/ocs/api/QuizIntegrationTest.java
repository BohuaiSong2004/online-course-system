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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quizzes, certificates and full-text search, against the demo seed. */
@SpringBootTest(properties = "ocs.rate-limit.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class QuizIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder b, String token, Object payload) throws Exception {
        if (token != null) {
            b = b.header("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            b = b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload));
        }
        return mvc.perform(b);
    }

    private String newLearner() throws Exception {
        return body(send(post("/api/auth/register"), null, Map.of("email", "q-" + UUID.randomUUID() + "@example.com",
                "password", "password123", "displayName", "Quiz Taker")).andExpect(status().isCreated())).get("token").asText();
    }

    private Map<String, Long> answers(JsonNode quiz, int... optionIndexes) {
        Map<String, Long> a = new HashMap<>();
        for (int i = 0; i < optionIndexes.length; i++) {
            a.put(quiz.get(i).get("id").asText(), quiz.get(i).get("options").get(optionIndexes[i]).get("id").asLong());
        }
        return a;
    }

    @Test
    void quizGatesCompletionAndCourseIssuesVerifiableCertificate() throws Exception {
        String token = newLearner();
        JsonNode detail = body(send(get("/api/courses/workplace-security-essentials"), token, null));
        long courseId = detail.at("/course/id").asLong();
        send(post("/api/courses/" + courseId + "/enroll"), token, null).andExpect(status().isOk());
        long quizChapter = detail.at("/chapters/0/id").asLong();

        JsonNode view = body(send(get("/api/chapters/" + quizChapter), token, null).andExpect(status().isOk()));
        JsonNode quiz = view.get("quiz");
        assertThat(quiz.size()).isEqualTo(3);
        assertThat(quiz.get(0).get("options").get(0).has("correct")).isFalse(); // answer key never sent

        send(post("/api/chapters/" + quizChapter + "/complete"), token, null).andExpect(status().isBadRequest());

        // 1/3 correct < 70%: fail, no answer key revealed
        JsonNode fail = body(send(post("/api/chapters/" + quizChapter + "/quiz/attempts"), token,
                Map.of("answers", answers(quiz, 2, 0, 0))).andExpect(status().isOk()));
        assertThat(fail.get("passed").asBoolean()).isFalse();
        assertThat(fail.get("score").asInt()).isEqualTo(1);
        assertThat(fail.at("/results/0/correctOptionId").isNull()).isTrue();
        assertThat(fail.get("progress").isNull()).isTrue();

        JsonNode pass = body(send(post("/api/chapters/" + quizChapter + "/quiz/attempts"), token,
                Map.of("answers", answers(quiz, 2, 1, 1))).andExpect(status().isOk()));
        assertThat(pass.get("passed").asBoolean()).isTrue();
        assertThat(pass.at("/results/0/explanation").asText()).isNotBlank();
        assertThat(pass.at("/progress/completedChapters").asInt()).isEqualTo(1);
        send(get("/api/chapters/" + quizChapter), token, null).andExpect(jsonPath("$.bestAttempt.passed").value(true));

        JsonNode last = null;
        for (int i = 1; i < detail.get("chapters").size(); i++) {
            last = body(send(post("/api/chapters/" + detail.at("/chapters/" + i + "/id").asLong() + "/complete"), token, null)
                    .andExpect(status().isOk()));
        }
        String code = last.get("certificateCode").asText();
        assertThat(code).matches("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}");

        send(get("/api/certificates/" + code.toLowerCase()), null, null) // public, case-insensitive
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerName").value("Quiz Taker"))
                .andExpect(jsonPath("$.courseTitle").value("Workplace Security Essentials"));
        send(get("/api/certificates/NOPE00000000"), null, null).andExpect(status().isNotFound());
        send(get("/api/me/certificates"), token, null).andExpect(jsonPath("$[0].code").value(code));
        send(get("/api/courses/workplace-security-essentials"), token, null)
                .andExpect(jsonPath("$.viewer.certificateCode").value(code));
    }

    @Test
    void studioQuizEditingValidatesAnswerKey() throws Exception {
        String instructor = body(send(post("/api/auth/login"), null,
                Map.of("email", "instructor@demo.dev", "password", "password123"))).get("token").asText();
        JsonNode course = body(send(post("/api/studio/courses"), instructor, Map.of("title", "Quiz course " + UUID.randomUUID(),
                "summary", "s", "category", "Testing", "level", "BEGINNER")).andExpect(status().isCreated()));
        JsonNode ch = body(send(post("/api/studio/courses/" + course.get("id").asLong() + "/chapters"), instructor,
                Map.of("title", "With video", "content", "x", "durationMin", 5,
                        "videoUrl", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")).andExpect(status().isCreated()));
        long chId = ch.get("id").asLong();
        assertThat(ch.get("videoUrl").asText()).startsWith("https://");
        send(put("/api/studio/chapters/" + chId), instructor, Map.of("title", "Bad", "videoUrl", "javascript:alert(1)"))
                .andExpect(status().isBadRequest());

        Map<String, Object> twoCorrect = Map.of("passPercent", 80, "questions", List.of(Map.of("prompt", "Q?",
                "options", List.of(Map.of("text", "a", "correct", true), Map.of("text", "b", "correct", true)))));
        send(put("/api/studio/chapters/" + chId + "/quiz"), instructor, twoCorrect).andExpect(status().isBadRequest());

        Map<String, Object> ok = Map.of("passPercent", 80, "questions", List.of(Map.of("prompt", "Q?", "explanation", "because",
                "options", List.of(Map.of("text", "a", "correct", false), Map.of("text", "b", "correct", true)))));
        send(put("/api/studio/chapters/" + chId + "/quiz"), instructor, ok)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passPercent").value(80))
                .andExpect(jsonPath("$.questions[0].options[1].correct").value(true));
        send(get("/api/studio/courses/" + course.get("id").asLong()), instructor, null)
                .andExpect(jsonPath("$.chapters[0].quizQuestionCount").value(1));
    }

    @Test
    void fullTextSearchRanksAndFallsBack() throws Exception {
        send(get("/api/courses?q=spring boot"), null, null)
                .andExpect(jsonPath("$.items[0].slug").value("java-spring-boot-fundamentals"));
        send(get("/api/courses?q=phishing"), null, null) // only in the description/summary
                .andExpect(jsonPath("$.items[0].slug").value("workplace-security-essentials"));
        send(get("/api/courses?q=SQL"), null, null)
                .andExpect(jsonPath("$.items[0].slug").value("sql-for-analysts"));
        send(get("/api/courses?q=ja"), null, null) // too short for the full-text index: LIKE fallback
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("java-spring-boot-fundamentals"));
        send(get("/api/courses?q=\"+-*()~<>"), null, null).andExpect(status().isOk()); // operators stripped
    }
}
