package com.bohuai.ocs.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
        return body(e.status(), e.code(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", "Malformed request", null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException e) {
        return body(HttpStatus.CONFLICT, "conflict", "Resource already exists", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        if (e instanceof ErrorResponse er) {
            // Spring MVC's own exceptions (404 no handler, 405 method, 415 media type, ...)
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return body(status, status.name().toLowerCase(), status.getReasonPhrase(), null);
        }
        log.error("Unhandled error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong", null);
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message,
                                                            Map<String, String> fields) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", code);
        b.put("message", message);
        if (fields != null) {
            b.put("fields", fields);
        }
        return ResponseEntity.status(status).body(b);
    }
}
