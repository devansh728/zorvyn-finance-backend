package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.record.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateRecordRequest(
        @NotNull UUID categoryId,

        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotNull TransactionType type,

        @NotNull @PastOrPresent(message = "Transaction date cannot be in the future")
        Instant transactionDate,

        @Size(max = 1000) String notes
) {}
