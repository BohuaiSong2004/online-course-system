package com.bohuai.ocs.api;

import com.bohuai.ocs.cache.OcsCache;
import com.bohuai.ocs.ratelimit.RateLimiter;
import com.bohuai.ocs.ratelimit.RedisRateLimiter;
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
        assertThat(limiter).isInstanceOf(RedisRateLimiter.class);
        // Two limiter instances = two API nodes sharing one Redis. Refill is negligible, so exactly the
        // burst capacity is allowed in total, no matter which node serves each request.
        RedisRateLimiter nodeA = new RedisRateLimiter(redis, 5, 0.001);
        RedisRateLimiter nodeB = new RedisRateLimiter(redis, 5, 0.001);
        String client = "test-" + UUID.randomUUID();
        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if ((i % 2 == 0 ? nodeA : nodeB).tryConsume(client).allowed()) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(5);
        RateLimiter.Decision refused = nodeA.tryConsume(client);
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterMillis()).isPositive();
    }
}
