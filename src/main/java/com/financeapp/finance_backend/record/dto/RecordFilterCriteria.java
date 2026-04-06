package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.record.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Optional filters used when querying financial records")
public record RecordFilterCriteria(
        @Schema(description = "Filter by transaction type", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
        TransactionType type,

        @Schema(description = "Filter by category identifier", example = "46b6da6b-a10b-4a2f-95ba-a0f2bc43e301")
        UUID categoryId,

        @Schema(description = "Filter start date-time (inclusive, UTC)", example = "2026-04-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,

        @Schema(description = "Filter end date-time (inclusive, UTC)", example = "2026-04-30T23:59:59Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,

        @Schema(description = "Case-insensitive text search against notes", example = "rent")
        String search
) {}
