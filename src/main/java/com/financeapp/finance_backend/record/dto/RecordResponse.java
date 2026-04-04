package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.category.dto.CategoryResponse;
import com.financeapp.finance_backend.record.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordResponse(
        UUID id,
        UUID createdBy,
        CategoryResponse category,
        BigDecimal amount,
        TransactionType type,
        Instant transactionDate,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        int version
) {}
