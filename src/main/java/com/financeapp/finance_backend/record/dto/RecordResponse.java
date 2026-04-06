package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.category.dto.CategoryResponse;
import com.financeapp.finance_backend.record.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Financial record response")
public record RecordResponse(
        @Schema(description = "Record identifier", example = "f32dd26f-7c66-43e8-9162-dd26f080829b")
        UUID id,

        @Schema(description = "User who created the record", example = "f8bd6314-3ecc-4fd3-bf45-f8be61ad2a36")
        UUID createdBy,

        @Schema(description = "Category details")
        CategoryResponse category,

        @Schema(description = "Record amount", example = "245.90")
        BigDecimal amount,

        @Schema(description = "Transaction type", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
        TransactionType type,

        @Schema(description = "Transaction date-time (UTC)", example = "2026-04-03T10:30:00Z")
        Instant transactionDate,

        @Schema(description = "Optional notes", example = "Team lunch")
        String notes,

        @Schema(description = "Creation timestamp (UTC)", example = "2026-04-03T10:31:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp (UTC)", example = "2026-04-04T09:12:00Z")
        Instant updatedAt,

        @Schema(description = "Optimistic lock version", example = "4")
        int version
) {}
