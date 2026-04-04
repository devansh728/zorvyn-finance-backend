package com.financeapp.finance_backend.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ErrorDetail {
    private String code;
    private String details;
    private List<ValidationError> validationErrors;

    @Data
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}
