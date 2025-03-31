package com.hrms.tenant_management.utils;

import com.hrms.tenant_management.dto.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        BaseResponse<Map<String, String>> response = new BaseResponse<>();
        response.setCode(ErrorCodes.BAD_REQUEST);
        response.setMessage("Validation failed" + errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.setCode(ErrorCodes.ILLEGAL_ARGUMENTS);
        response.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<BaseResponse<Void>> handleKeycloakException(KeycloakException e) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.setCode(e.getErrorCode());
        response.setMessage(e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle client-side HTTP errors
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpClientErrorException(HttpClientErrorException ex) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.setCode(ex.getStatusCode().value());
        response.setMessage("Client error: " + ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    // Handle server-side HTTP errors
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpServerErrorException(HttpServerErrorException ex) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.setCode(ex.getStatusCode().value());
        response.setMessage("Server error: " + ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    // Handle all other exceptions (generic exception handling)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage("An unexpected error occurred: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
