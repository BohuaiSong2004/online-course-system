package com.bohuai.ocs.cache;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * MyBatis second-level cache used by the catalog mappers.
 * <p>
 * Single instance (default): an in-process LRU of serialized copies, flushed every 5 minutes.
 * Several instances behind a load balancer: set {@code OCS_REDIS_ENABLED=true} and every instance
 * shares one Redis-backed cache, so a write on one node invalidates the cached catalog everywhere.
 * <p>
 * MyBatis creates cache objects itself (no Spring injection), so the Redis backend is handed over by
 * {@link CacheBackendRegistrar} at startup. Until then — and when Redis is disabled — the local cache
 * is used.
 */
public class OcsCache implements Cache {

    static final long TTL_SECONDS = 300;
    private static volatile RedisCacheStore redis;

    private final String id;
    private final Cache local;

    public OcsCache(String id) {
        this.id = id;
        ScheduledCache scheduled = new ScheduledCache(new LruCache(new PerpetualCache(id)));
        scheduled.setClearInterval(TTL_SECONDS * 1000);
        this.local = new SynchronizedCache(new SerializedCache(scheduled));
    }

    static void useRedis(RedisCacheStore store) {
        redis = store;
    }

    public static boolean redisEnabled() {
        return redis != null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void putObject(Object key, Object value) {
        RedisCacheStore r = redis;
        if (r != null) {
            r.put(id, key, value);
        } else {
            local.putObject(key, value);
        }
    }

    @Override
    public Object getObject(Object key) {
        RedisCacheStore r = redis;
        return r != null ? r.get(id, key) : local.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        RedisCacheStore r = redis;
        if (r != null) {
            r.remove(id, key);
            return null;
        }
        return local.removeObject(key);
    }

    @Override
    public void clear() {
        RedisCacheStore r = redis;
        if (r != null) {
            r.clear(id);
        } else {
            local.clear();
        }
    }

    @Override
    public int getSize() {
        RedisCacheStore r = redis;
        return r != null ? r.size(id) : local.getSize();
    }
}
