package com.financeapp.finance_backend.common.exception;

import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(Exception ex) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                        ErrorDetail errorDetail = ErrorDetail.builder()
                                        .code("UNAUTHORIZED")
                                        .details("Authentication token is missing or invalid.")
                                        .build();

                        return new ResponseEntity<>(ApiResponse.error(errorDetail, "Unauthorized"),
                                        HttpStatus.UNAUTHORIZED);
                }
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code("FORBIDDEN")
                                .details("You do not have sufficient privileges to access this resource.")
                                .build();

                return new ResponseEntity<>(ApiResponse.error(errorDetail, "Access Denied"), HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler({ BadCredentialsException.class, AuthenticationException.class })
        public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(Exception ex) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code("UNAUTHORIZED")
                                .details("Invalid email or password.")
                                .build();

                return new ResponseEntity<>(ApiResponse.error(errorDetail, "Authentication failed"),
                                HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex,
                        WebRequest request) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code(ex.getErrorCode())
                                .details(ex.getMessage())
                                .build();
                return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex,
                        WebRequest request) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code(ex.getErrorCode())
                                .details(ex.getMessage())
                                .build();
                return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(CustomConcurrentModificationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConcurrentModificationException(
                        CustomConcurrentModificationException ex, WebRequest request) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code(ex.getErrorCode())
                                .details(ex.getMessage())
                                .build();
                return new ResponseEntity<>(ApiResponse.error(errorDetail, ex.getMessage()), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(BusinessRuleException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusinessRuleException(BusinessRuleException ex,
                        WebRequest request) {
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
                return new ResponseEntity<>(ApiResponse.error(errorDetail, "Validation failed"),
                                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code("VALIDATION_FAILED")
                                .details("Constraint validation failed: " + ex.getMessage())
                                .build();
                return new ResponseEntity<>(ApiResponse.error(errorDetail, "Validation failed"),
                                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
                ErrorDetail errorDetail = ErrorDetail.builder()
                                .code("INTERNAL_SERVER_ERROR")
                                .details("An unexpected error occurred")
                                .build();
                return new ResponseEntity<>(ApiResponse.error(errorDetail, "An unexpected error occurred"),
                                HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
