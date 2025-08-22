package com.bohuai.ocs.ratelimit;

/** Token-bucket limiter keyed by client. */
public interface RateLimiter {

    /**
     * @param allowed          whether the request may proceed (a token was taken)
     * @param remaining        whole tokens left after this request
     * @param retryAfterMillis when refused, how long until a token is available
     */
    record Decision(boolean allowed, int remaining, long retryAfterMillis) {
    }

    Decision tryConsume(String clientKey);
}
