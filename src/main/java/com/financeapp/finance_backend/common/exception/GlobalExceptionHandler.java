package com.financeapp.finance_backend.common.exception;

import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code(ex.getErrorCode())
                .details(ex.getMessage())
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code(ex.getErrorCode())
                .details(ex.getMessage())
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CustomConcurrentModificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentModificationException(CustomConcurrentModificationException ex, WebRequest request) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code(ex.getErrorCode())
                .details(ex.getMessage())
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code(ex.getErrorCode())
                .details(ex.getMessage())
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.valueOf(422));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("VALIDATION_FAILED")
                .details("Input validation failed")
                .validationErrors(ex.getBindingResult().getAllErrors().stream()
                        .map(error -> ErrorDetail.ValidationError.builder()
                                .field(((FieldError) error).getField())
                                .message(error.getDefaultMessage())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, "Validation failed"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
         ErrorDetail errorDetail = ErrorDetail.builder()
                .code("VALIDATION_FAILED")
                .details("Constraint validation failed: " + ex.getMessage())
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, "Validation failed"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("INTERNAL_SERVER_ERROR")
                .details("An unexpected error occurred")
                .build();
        return new ResponseEntity<>(ApiResponse.error(errorDetail, "An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
