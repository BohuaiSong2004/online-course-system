package com.bohuai.ocs.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRateLimiterTest {

    @Test
    void allowsBurstThenThrottles() {
        LocalRateLimiter.Bucket bucket = new LocalRateLimiter.Bucket(3, 1.0);
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isPositive().isLessThanOrEqualTo(1000);
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        LocalRateLimiter.Bucket bucket = new LocalRateLimiter.Bucket(1, 2.0); // one token every 500ms
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isPositive();
        Thread.sleep(700);
        assertThat(bucket.tryConsume()).isZero();
    }

    @Test
    void clientsHaveSeparateBuckets() {
        LocalRateLimiter limiter = new LocalRateLimiter(1, 0.001);
        assertThat(limiter.tryConsume("a").allowed()).isTrue();
        assertThat(limiter.tryConsume("a").allowed()).isFalse();
        assertThat(limiter.tryConsume("b").allowed()).isTrue();
    }

    @Test
    void evictionKeepsThrottledClients() {
        LocalRateLimiter limiter = new LocalRateLimiter(1, 0.001);
        limiter.tryConsume("busy");
        limiter.evictIdle(); // "busy" has an empty bucket, so it must survive eviction
        assertThat(limiter.trackedClients()).isEqualTo(1);
    }
}
