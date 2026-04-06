package com.financeapp.finance_backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Error payload returned when an API call fails")
public class ErrorDetail {
    @Schema(description = "Machine-readable error code", example = "VALIDATION_FAILED")
    private String code;

    @Schema(description = "Detailed error description", example = "Input validation failed")
    private String details;

    @Schema(description = "Field-level validation errors, when applicable")
    private List<ValidationError> validationErrors;

    @Data
    @Builder
    @Schema(description = "Validation error for a specific field")
    public static class ValidationError {
        @Schema(description = "Field name that failed validation", example = "email")
        private String field;

        @Schema(description = "Validation message for the field", example = "must be a well-formed email address")
        private String message;
    }
}
