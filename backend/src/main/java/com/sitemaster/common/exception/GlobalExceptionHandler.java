package com.sitemaster.common.exception;

import com.sitemaster.common.util.MessageResolver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates every exception into the shared {@link ErrorResponse} body with
 * 400 (validation), 401, 403, 404, 409 (duplicate), 422 (business rule) or 500.
 * Messages are resolved from ValidationMessages.properties.
 * <p>
 * OOP concept: Polymorphism - all business exceptions are handled through their
 * common supertype {@link ApplicationException}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageResolver messages;

    public GlobalExceptionHandler(MessageResolver messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // A binding failure (e.g. "abc" for a number) has no friendly default message.
            String message = error.isBindingFailure()
                    ? messages.get("error.typeMismatch", error.getField())
                    : error.getDefaultMessage();
            fieldErrors.putIfAbsent(error.getField(), message);
        }
        List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        return build(HttpStatus.BAD_REQUEST, messages.get("error.validation"), fieldErrors, globalErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        // Constraints on @RequestParam / @RequestPart (e.g. @ValidFile on "file"):
        // report each under its request parameter name so the form can show it.
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        List<String> globalErrors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getAllValidationResults()) {
            String name = parameterName(result.getMethodParameter());
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error.getDefaultMessage() == null) {
                    continue;
                }
                if (name == null) {
                    globalErrors.add(error.getDefaultMessage());
                } else {
                    fieldErrors.putIfAbsent(name, error.getDefaultMessage());
                }
            }
        }
        return build(HttpStatus.BAD_REQUEST, messages.get("error.validation"), fieldErrors, globalErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.putIfAbsent(lastNode(violation.getPropertyPath()), violation.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, messages.get("error.validation"), fieldErrors, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return simple(HttpStatus.BAD_REQUEST, messages.get("error.malformedRequest"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return simple(HttpStatus.BAD_REQUEST, messages.get("error.typeMismatch", ex.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return simple(HttpStatus.BAD_REQUEST, messages.get("error.missingParameter", ex.getParameterName()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        String message = messages.get("file.empty.field");
        return build(HttpStatus.BAD_REQUEST, messages.get("error.validation"),
                Map.of(ex.getRequestPartName(), message), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return simple(HttpStatus.BAD_REQUEST, messages.get("file.tooLarge"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return simple(HttpStatus.METHOD_NOT_ALLOWED, messages.get("error.methodNotAllowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return simple(HttpStatus.NOT_FOUND, messages.get("error.notFound"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return simple(HttpStatus.FORBIDDEN, messages.get("error.forbidden"));
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplication(ApplicationException ex) {
        String message = messages.get(ex.getMessageKey(), ex.getArgs());
        Map<String, String> fieldErrors = ex.getField() == null ? Map.of() : Map.of(ex.getField(), message);
        return build(ex.getStatus(), message, fieldErrors, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violated: {}", ex.getMostSpecificCause().getMessage());
        return simple(HttpStatus.CONFLICT, messages.get("error.dataConflict"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return simple(HttpStatus.INTERNAL_SERVER_ERROR, messages.get("error.internal"));
    }

    private ResponseEntity<ErrorResponse> simple(HttpStatus status, String message) {
        return build(status, message, Map.of(), List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                Map<String, String> fieldErrors, List<String> globalErrors) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, fieldErrors, new ArrayList<>(globalErrors)));
    }

    /** Name used by the client: the @RequestParam/@RequestPart value, else the Java parameter name. */
    private static String parameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        if (requestPart != null && !requestPart.name().isEmpty()) {
            return requestPart.name();
        }
        return parameter.getParameterName();
    }

    private String lastNode(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name == null ? "value" : name;
    }
}
