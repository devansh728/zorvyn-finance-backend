package com.financeapp.finance_backend.common.exception;

public class CustomConcurrentModificationException extends BusinessException {
    public CustomConcurrentModificationException(String message) {
        super(message, "CONCURRENT_MODIFICATION");
    }
}
