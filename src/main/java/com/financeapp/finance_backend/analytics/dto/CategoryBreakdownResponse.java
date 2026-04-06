package com.financeapp.finance_backend.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Category-wise contribution in analytics results")
public record CategoryBreakdownResponse(
        @Schema(description = "Category identifier", example = "46b6da6b-a10b-4a2f-95ba-a0f2bc43e301")
        UUID categoryId,

        @Schema(description = "Category name", example = "Groceries")
        String categoryName,

        @Schema(description = "Total amount for the category", example = "2140.75")
        BigDecimal total,

        @Schema(description = "Record count for the category", example = "16")
        long count,

        @Schema(description = "Percentage of overall total", example = "21.84")
        BigDecimal percentage
) {}
