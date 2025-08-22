package com.bohuai.ocs.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-process buckets: correct for a single instance (local development, one container). */
public class LocalRateLimiter implements RateLimiter {

    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final int capacity;
    private final double refillPerSecond;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LocalRateLimiter(int capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    @Override
    public Decision tryConsume(String clientKey) {
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            evictIdle();
        }
        Bucket b = buckets.computeIfAbsent(clientKey, k -> new Bucket(capacity, refillPerSecond));
        long wait = b.tryConsume();
        return new Decision(wait == 0, b.remaining(), wait);
    }

    /**
     * Memory guard: drops buckets that have refilled completely (clients that went quiet). Clearing
     * everything instead would hand a fresh burst to clients that are currently being throttled.
     */
    void evictIdle() {
        buckets.values().removeIf(Bucket::isFull);
    }

    int trackedClients() {
        return buckets.size();
    }

    static final class Bucket {
        private final int capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefill;

        Bucket(int capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000d;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        /** @return 0 if a token was consumed, otherwise milliseconds until one is available. */
        synchronized long tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            double missing = 1 - tokens;
            return (long) Math.ceil(missing / refillPerNano / 1_000_000d);
        }

        synchronized boolean isFull() {
            refill();
            return tokens >= capacity;
        }

        synchronized int remaining() {
            return (int) Math.floor(tokens);
        }

        private void refill() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
            lastRefill = now;
        }
    }
}
