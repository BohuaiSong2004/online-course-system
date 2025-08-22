package com.bohuai.ocs.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * One Redis hash per MyBatis namespace ({@code ocs:cache:<namespace>}); fields are SHA-256 hashes of
 * the MyBatis cache key, values are Java-serialized results. Clearing a namespace is a single DEL.
 * Redis errors degrade to cache misses instead of failing the request.
 */
public class RedisCacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheStore.class);

    private final RedisTemplate<String, byte[]> redis;

    public RedisCacheStore(RedisTemplate<String, byte[]> redis) {
        this.redis = redis;
    }

    void put(String ns, Object key, Object value) {
        try {
            String k = redisKey(ns);
            redis.opsForHash().put(k, field(key), serialize(value));
            redis.expire(k, Duration.ofSeconds(OcsCache.TTL_SECONDS));
        } catch (RuntimeException e) {
            log.warn("Redis cache put failed: {}", e.getMessage());
        }
    }

    Object get(String ns, Object key) {
        try {
            Object raw = redis.opsForHash().get(redisKey(ns), field(key));
            return raw == null ? null : deserialize((byte[]) raw);
        } catch (RuntimeException e) {
            log.warn("Redis cache get failed: {}", e.getMessage());
            return null;
        }
    }

    void remove(String ns, Object key) {
        try {
            redis.opsForHash().delete(redisKey(ns), field(key));
        } catch (RuntimeException e) {
            log.warn("Redis cache remove failed: {}", e.getMessage());
        }
    }

    void clear(String ns) {
        try {
            redis.delete(redisKey(ns));
        } catch (RuntimeException e) {
            // A failed invalidation could leave stale data for up to the TTL; make it visible.
            log.error("Redis cache clear failed for {}: {}", ns, e.getMessage());
        }
    }

    int size(String ns) {
        try {
            Long n = redis.opsForHash().size(redisKey(ns));
            return n == null ? 0 : n.intValue();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static String redisKey(String ns) {
        return "ocs:cache:" + ns;
    }

    private static String field(Object key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] serialize(Object value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Cached value is not serializable", e);
        }
    }

    private static Object deserialize(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            // Only our own model types and JDK collections/values may be read back.
            ois.setObjectInputFilter(info -> {
                Class<?> c = info.serialClass();
                if (c == null || c.isPrimitive() || c.isArray()) {
                    return java.io.ObjectInputFilter.Status.UNDECIDED;
                }
                String n = c.getName();
                return n.startsWith("com.bohuai.ocs.model.") || n.startsWith("java.")
                        ? java.io.ObjectInputFilter.Status.ALLOWED
                        : java.io.ObjectInputFilter.Status.REJECTED;
            });
            return ois.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read cached value", e);
        }
    }
}
