package com.bohuai.ocs.api;

import com.bohuai.ocs.cache.OcsCache;
import com.bohuai.ocs.ratelimit.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Redis mode: catalog cache lives in Redis and is invalidated there; rate limits are shared. */
@SpringBootTest(properties = {"ocs.redis.enabled=true", "ocs.rate-limit.enabled=false"})
@AutoConfigureMockMvc
@DirtiesContext
@EnabledIfEnvironmentVariable(named = "REDIS_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class RedisIntegrationTest {

    private static final String CATALOG_KEY = "ocs:cache:com.bohuai.ocs.mapper.CourseMapper";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    RateLimiter limiter;

    @Test
    void catalogCacheIsSharedInRedisAndInvalidatedOnWrite() throws Exception {
        assertThat(OcsCache.redisEnabled()).isTrue();
        redis.delete(CATALOG_KEY);
        mvc.perform(get("/api/courses/sql-for-analysts")).andExpect(status().isOk());
        java.util.Set<Object> before = redis.opsForHash().keys(CATALOG_KEY);
        assertThat(before).isNotEmpty();

        String token = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"instructor@demo.dev\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/studio/courses").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Redis course " + UUID.randomUUID(),
                                "summary", "s", "category", "Testing", "level", "BEGINNER"))))
                .andExpect(status().isCreated());
        // A write on any instance clears the shared namespace for all of them: none of the entries cached
        // before the write survive (the write's own transaction may cache fresh rows afterwards).
        assertThat(redis.opsForHash().keys(CATALOG_KEY)).doesNotContainAnyElementsOf(before);
    }

    @Test
    void redisRateLimiterSharesOneBucketPerClient() {
        String client = "test-" + UUID.randomUUID();
        int allowed = 0;
        for (int i = 0; i < 150; i++) {
            if (limiter.tryConsume(client).allowed()) {
                allowed++;
            }
        }
        assertThat(allowed).isBetween(100, 110); // burst of 100 plus whatever refilled during the loop
    }
}
