package com.tarotalk.chat.api;

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
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("NOT_FOUND".equals(ex.getCode())) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("errors", ex.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "invalid request", details));
    }
}
