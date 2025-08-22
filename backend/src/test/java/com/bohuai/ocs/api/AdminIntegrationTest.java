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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Users, teams, assignments and reports, against the demo seed (Mia manages Engineering and Sales). */
@SpringBootTest(properties = "ocs.rate-limit.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class AdminIntegrationTest {

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

    private String login(String email) throws Exception {
        return body(send(post("/api/auth/login"), null, Map.of("email", email, "password", "password123"))
                .andExpect(status().isOk())).get("token").asText();
    }

    private long userId(String admin, String q) throws Exception {
        return body(send(get("/api/admin/users?q=" + q), admin, null)).at("/items/0/id").asLong();
    }

    @Test
    void rolesAreEnforced() throws Exception {
        String learner = login("learner@demo.dev");
        String manager = login("manager@demo.dev");
        send(get("/api/admin/users"), learner, null).andExpect(status().isForbidden());
        send(get("/api/admin/reports/overview"), learner, null).andExpect(status().isForbidden());
        send(get("/api/admin/users"), manager, null).andExpect(status().isForbidden());
        send(post("/api/admin/teams"), manager, Map.of("name", "Nope")).andExpect(status().isForbidden());
        // managers only see the teams they manage
        send(get("/api/admin/teams"), manager, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", everyItem(isOneOf("Engineering", "Sales"))));
    }

    @Test
    void managerAssignsToOwnTeamOnlyAndLearnerSeesDueDate() throws Exception {
        String manager = login("manager@demo.dev");
        String admin = login("admin@demo.dev");
        long peopleOps = userId(admin, "Mila");      // People Ops: not managed by Mia
        LocalDate due = LocalDate.now().plusDays(10);

        JsonNode res = body(send(post("/api/admin/assignments"), manager,
                Map.of("courseId", 2, "teamId", 1, "userIds", List.of(peopleOps), "dueDate", due.toString()))
                .andExpect(status().isOk()));
        assertThat(res.get("created").asInt() + res.get("unchanged").asInt() + res.get("updated").asInt())
                .isGreaterThan(0);
        assertThat(res.get("skipped").asInt()).isEqualTo(1);

        // idempotent; changing the due date updates instead of duplicating
        send(post("/api/admin/assignments"), manager, Map.of("courseId", 2, "teamId", 1, "dueDate", due.toString()))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(0));
        send(post("/api/admin/assignments"), manager,
                Map.of("courseId", 2, "teamId", 1, "dueDate", due.plusDays(1).toString()))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated", greaterThan(0)));
        send(post("/api/admin/assignments"), manager, Map.of("courseId", 2, "teamId", 1, "dueDate", due.toString()))
                .andExpect(jsonPath("$.updated", greaterThan(0)));

        String leo = login("learner@demo.dev"); // Engineering
        JsonNode enrollments = body(send(get("/api/me/enrollments"), leo, null));
        boolean found = false;
        for (JsonNode e : enrollments) {
            if (e.get("courseId").asLong() == 2) {
                assertThat(e.get("dueDate").asText()).isEqualTo(due.toString());
                assertThat(e.get("assignedByName").asText()).isEqualTo("Mia Manager");
                found = true;
            }
        }
        assertThat(found).isTrue();

        send(post("/api/admin/assignments"), manager,
                Map.of("courseId", 2, "teamId", 1, "dueDate", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest());
        send(post("/api/admin/assignments"), manager, Map.of("courseId", 7, "teamId", 1))
                .andExpect(status().isBadRequest()); // draft course
    }

    @Test
    void reportsAndCsvAreScoped() throws Exception {
        String manager = login("manager@demo.dev");
        send(get("/api/admin/reports/overview"), manager, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", greaterThan(0)))
                .andExpect(jsonPath("$.overdue", greaterThan(0)))
                .andExpect(jsonPath("$.teams[*].name", everyItem(isOneOf("Engineering", "Sales"))));
        send(get("/api/admin/assignments?status=OVERDUE"), manager, null)
                .andExpect(jsonPath("$[*].status", everyItem(isOneOf("OVERDUE"))));
        send(get("/api/admin/reports/assignments.csv"), manager, null)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("learner,email,team,course,status")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Mila Rossi"))));
    }

    @Test
    void adminManagesUsersAndTeams() throws Exception {
        String admin = login("admin@demo.dev");
        long adminId = userId(admin, "Ada");
        send(put("/api/admin/users/" + adminId), admin, Map.of("role", "LEARNER", "active", true))
                .andExpect(status().isBadRequest()); // no self-demotion

        String email = "emp-" + UUID.randomUUID() + "@example.com";
        JsonNode reg = body(send(post("/api/auth/register"), null,
                Map.of("email", email, "password", "password123", "displayName", "New Hire")).andExpect(status().isCreated()));
        long id = reg.at("/user/id").asLong();
        String refresh = reg.get("refreshToken").asText();

        JsonNode team = body(send(post("/api/admin/teams"), admin,
                Map.of("name", "Team " + UUID.randomUUID().toString().substring(0, 6), "managerId", 1))
                .andExpect(status().isCreated()));
        send(post("/api/admin/teams"), admin, Map.of("name", team.get("name").asText()))
                .andExpect(status().isConflict());
        send(post("/api/admin/teams"), admin, Map.of("name", "Bad manager", "managerId", 3))
                .andExpect(status().isBadRequest()); // Leo is a learner

        send(put("/api/admin/users/" + id), admin,
                Map.of("role", "INSTRUCTOR", "teamId", team.get("id").asLong(), "active", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.teamName").value(team.get("name").asText()));
        // role change revoked the old session's refresh token
        send(post("/api/auth/refresh"), null, Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());

        send(put("/api/admin/users/" + id), admin, Map.of("role", "INSTRUCTOR", "active", false))
                .andExpect(jsonPath("$.active").value(false));
        send(post("/api/auth/login"), null, Map.of("email", email, "password", "password123"))
                .andExpect(status().isForbidden());
    }
}
