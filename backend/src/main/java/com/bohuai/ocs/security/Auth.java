package com.bohuai.ocs.security;

import com.bohuai.ocs.common.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/** Request-scoped access to the authenticated caller. */
@Component
public class Auth {

    public Optional<CurrentUser> current() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object u = attrs.getAttribute(AuthFilter.ATTR, RequestAttributes.SCOPE_REQUEST);
        return Optional.ofNullable((CurrentUser) u);
    }

    public CurrentUser require() {
        return current().orElseThrow(() -> ApiException.unauthorized("Sign in required"));
    }

    public CurrentUser requireAuthor() {
        CurrentUser u = require();
        if (!u.role().canAuthor()) {
            throw ApiException.forbidden("Instructor role required");
        }
        return u;
    }
}
