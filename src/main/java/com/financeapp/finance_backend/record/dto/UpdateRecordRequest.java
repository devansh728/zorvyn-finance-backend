package com.financeapp.finance_backend.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request payload for partially updating a financial record")
public record UpdateRecordRequest(
        @Schema(description = "Updated category identifier", example = "46b6da6b-a10b-4a2f-95ba-a0f2bc43e301")
        UUID categoryId,

        @Schema(description = "Updated transaction amount", example = "1299.99")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Schema(description = "Updated transaction timestamp (UTC)", example = "2026-04-04T16:45:00Z")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        Instant transactionDate,

        @Schema(description = "Updated notes", example = "Adjusted amount after invoice correction")
        @Size(max = 1000) String notes,

        @Schema(description = "Current record version for optimistic locking", example = "2")
        @NotNull(message = "Version is required for conflict detection")
        Integer version
) {}
