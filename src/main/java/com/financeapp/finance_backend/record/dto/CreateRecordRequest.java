package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.record.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request payload for creating a financial record")
public record CreateRecordRequest(
        @Schema(description = "Category identifier", example = "46b6da6b-a10b-4a2f-95ba-a0f2bc43e301")
        @NotNull UUID categoryId,

        @Schema(description = "Transaction amount", example = "1250.75")
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Schema(description = "Transaction type", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
        @NotNull TransactionType type,

        @Schema(description = "Transaction timestamp (UTC)", example = "2026-04-04T14:20:00Z")
        @NotNull @PastOrPresent(message = "Transaction date cannot be in the future")
        Instant transactionDate,

        @Schema(description = "Optional notes", example = "Monthly rent payment")
        @Size(max = 1000) String notes
) {}
