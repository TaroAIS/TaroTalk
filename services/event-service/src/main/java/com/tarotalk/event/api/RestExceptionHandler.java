package com.tarotalk.event.api;

import com.tarotalk.common.api.ErrorResponse;
import com.tarotalk.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(resolveStatus(ex.getCode()))
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("errors", ex.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "invalid request", details));
    }

    private HttpStatus resolveStatus(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        String normalized = code.trim().toUpperCase();
        if ("NOT_FOUND".equals(normalized)) {
            return HttpStatus.NOT_FOUND;
        }
        if (normalized.startsWith("FORBIDDEN")) {
            return HttpStatus.FORBIDDEN;
        }
        if (normalized.startsWith("UNAUTHORIZED")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if ("VALIDATION_ERROR".equals(normalized)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
