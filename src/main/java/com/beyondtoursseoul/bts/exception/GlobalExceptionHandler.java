package com.beyondtoursseoul.bts.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException e,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", e.getStatusCode().value());
        body.put("error", e.getStatusCode().toString());
        body.put("message", e.getReason());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", e.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * JSON 본문 역직렬화 실패(타입 불일치, 잘린 JSON 등) 시 기본 메시지가 빈약해 원인을 같이 내려준다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        Throwable root = e.getMostSpecificCause() != null ? e.getMostSpecificCause() : e;
        String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
        log.warn("[HTTP 400] JSON parse/binding failed path={} detail={}", request.getRequestURI(), detail);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "요청 본문(JSON)을 읽을 수 없습니다: " + (detail != null ? detail : "unknown"));
        body.put("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }
}
