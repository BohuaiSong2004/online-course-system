package com.bohuai.ocs.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Cluster-wide token bucket: one Redis hash per client, updated atomically by a Lua script that uses
 * the Redis server clock, so every API instance enforces the same budget. If Redis is unreachable the
 * limiter fails open (requests are allowed and a warning is logged): API Gateway still enforces the
 * edge limits.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])            -- tokens per millisecond
            local t = redis.call('TIME')
            local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
            local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
            local tokens = tonumber(data[1]) or capacity
            local ts = tonumber(data[2]) or now
            tokens = math.min(capacity, tokens + math.max(0, now - ts) * rate)
            local allowed, wait = 0, 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            else
              wait = math.ceil((1 - tokens) / rate)
            end
            redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'ts', tostring(now))
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate) + 1000)
            return {allowed, math.floor(tokens), wait}
            """, List.class);

    private final StringRedisTemplate redis;
    private final String capacity;
    private final String ratePerMs;

    public RedisRateLimiter(StringRedisTemplate redis, int capacity, double refillPerSecond) {
        this.redis = redis;
        this.capacity = String.valueOf(capacity);
        this.ratePerMs = String.valueOf(refillPerSecond / 1000d);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Decision tryConsume(String clientKey) {
        try {
            List<Long> r = redis.execute(SCRIPT, List.of("ocs:ratelimit:" + clientKey), capacity, ratePerMs);
            return new Decision(r.get(0) == 1L, r.get(1).intValue(), r.get(2));
        } catch (RuntimeException e) {
            log.warn("Redis rate limiter unavailable, allowing request: {}", e.getMessage());
            return new Decision(true, -1, 0);
        }
    }
}
