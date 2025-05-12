package com.bohuai.ocs.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bohuai.ocs.security.AuthFilter;
import com.bohuai.ocs.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OcsProperties props;

    public WebConfig(OcsProperties props) {
        this.props = props;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(ObjectMapper mapper) {
        FilterRegistrationBean<RateLimitFilter> reg =
                new FilterRegistrationBean<>(new RateLimitFilter(props.rateLimit(), mapper));
        reg.addUrlPatterns("/api/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(JwtService jwtService) {
        FilterRegistrationBean<AuthFilter> reg =
                new FilterRegistrationBean<>(new AuthFilter(jwtService, props.jwt().cookieName(), props.cors().allowedOrigins()));
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
