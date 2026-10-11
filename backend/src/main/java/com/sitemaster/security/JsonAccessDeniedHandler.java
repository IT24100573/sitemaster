package com.sitemaster.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns 403 JSON when a signed-in user calls an API their role may not use,
 * or when the CSRF token is missing or wrong.
 * <p>
 * OOP concept: Polymorphism - an implementation of Spring's AccessDeniedHandler.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter writer;

    public JsonAccessDeniedHandler(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String key = accessDeniedException instanceof CsrfException ? "error.csrf" : "error.forbidden";
        writer.write(response, HttpStatus.FORBIDDEN, key);
    }
}
