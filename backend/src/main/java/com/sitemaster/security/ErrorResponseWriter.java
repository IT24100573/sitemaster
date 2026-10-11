package com.sitemaster.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitemaster.common.exception.ErrorResponse;
import com.sitemaster.common.util.MessageResolver;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the shared JSON error body from inside the security filter chain,
 * where @RestControllerAdvice does not apply.
 * <p>
 * OOP concept: Encapsulation - one place serialises security errors.
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final MessageResolver messages;

    public ErrorResponseWriter(ObjectMapper objectMapper, MessageResolver messages) {
        this.objectMapper = objectMapper;
        this.messages = messages;
    }

    public void write(HttpServletResponse response, HttpStatus status, String messageKey) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status.value(), messages.get(messageKey)));
    }
}
