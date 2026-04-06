package com.financeapp.finance_backend.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Time bucket values used in trend analytics")
public record TrendDataPoint(
        @Schema(description = "Start of the period bucket (UTC)", example = "2026-04-01T00:00:00Z")
        Instant period,

        @Schema(description = "Income total in the bucket", example = "3200.00")
        BigDecimal income,

        @Schema(description = "Expense total in the bucket", example = "2750.25")
        BigDecimal expense,

        @Schema(description = "Net value in the bucket", example = "449.75")
        BigDecimal net
) {}
