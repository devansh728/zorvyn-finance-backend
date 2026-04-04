package com.financeapp.finance_backend.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue), "NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND");
    }
}
