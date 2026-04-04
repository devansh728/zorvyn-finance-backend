package com.financeapp.finance_backend.common.exception;

public class BusinessRuleException extends BusinessException {
    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }
}
