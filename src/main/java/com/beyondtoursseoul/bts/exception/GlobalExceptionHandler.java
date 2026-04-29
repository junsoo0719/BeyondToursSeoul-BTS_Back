package com.beyondtoursseoul.bts.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientResponseException(
            RestClientResponseException e,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", e.getStatusCode().value());
        body.put("error", e.getStatusText());
        body.put("message", e.getResponseBodyAsString());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", e.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }
}
