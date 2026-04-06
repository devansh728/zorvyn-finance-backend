package com.financeapp.finance_backend.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Aggregated financial summary for a date range")
public record SummaryResponse(
        @Schema(description = "Total income amount", example = "15000.00")
        BigDecimal totalIncome,

        @Schema(description = "Total expense amount", example = "9800.40")
        BigDecimal totalExpense,

        @Schema(description = "Net balance (income - expense)", example = "5199.60")
        BigDecimal netBalance,

        @Schema(description = "Number of records included in the summary", example = "84")
        long recordCount
) {}
