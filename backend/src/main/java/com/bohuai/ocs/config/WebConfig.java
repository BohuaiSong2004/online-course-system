package com.bohuai.ocs.config;

import com.bohuai.ocs.observability.RequestIdFilter;
import com.bohuai.ocs.ratelimit.LocalRateLimiter;
import com.bohuai.ocs.ratelimit.RateLimiter;
import com.bohuai.ocs.ratelimit.RedisRateLimiter;
import com.bohuai.ocs.security.AuthFilter;
import com.bohuai.ocs.security.JwtService;
import com.bohuai.ocs.security.OriginPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final OcsProperties props;

    public WebConfig(OcsProperties props) {
        this.props = props;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Redis-backed (shared by all instances) when OCS_REDIS_ENABLED=true, in-process otherwise. */
    @Bean
    public RateLimiter rateLimiter(@Value("${ocs.redis.enabled:false}") boolean redisEnabled,
                                   ObjectProvider<StringRedisTemplate> redis) {
        OcsProperties.RateLimit rl = props.rateLimit();
        if (redisEnabled) {
            log.info("Rate limiter: Redis (shared across instances)");
            return new RedisRateLimiter(redis.getObject(), rl.capacity(), rl.refillPerSecond());
        }
        return new LocalRateLimiter(rl.capacity(), rl.refillPerSecond());
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> reg = new FilterRegistrationBean<>(new RequestIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(0);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(ObjectMapper mapper, RateLimiter limiter,
                                                                   MeterRegistry meters) {
        FilterRegistrationBean<RateLimitFilter> reg =
                new FilterRegistrationBean<>(new RateLimitFilter(props.rateLimit(), mapper, limiter, meters));
        reg.addUrlPatterns("/api/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(JwtService jwtService, OriginPolicy originPolicy) {
        FilterRegistrationBean<AuthFilter> reg =
                new FilterRegistrationBean<>(new AuthFilter(jwtService, props.jwt().cookieName(), originPolicy));
        reg.addUrlPatterns("/api/*");
        reg.setOrder(2);
        return reg;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
