package com.bohuai.ocs.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/** Activates the shared Redis cache backend when {@code ocs.redis.enabled=true}. */
@Configuration
@ConditionalOnProperty(name = "ocs.redis.enabled", havingValue = "true")
public class CacheBackendRegistrar {

    private static final Logger log = LoggerFactory.getLogger(CacheBackendRegistrar.class);

    @Bean
    public RedisTemplate<String, byte[]> ocsRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, byte[]> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(RedisSerializer.string());
        t.setHashKeySerializer(RedisSerializer.string());
        t.setValueSerializer(RedisSerializer.byteArray());
        t.setHashValueSerializer(RedisSerializer.byteArray());
        t.afterPropertiesSet();
        OcsCache.useRedis(new RedisCacheStore(t));
        log.info("MyBatis second-level cache: Redis (shared across instances)");
        return t;
    }
}
