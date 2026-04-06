package com.financeapp.finance_backend.category.dto;

import com.financeapp.finance_backend.category.entity.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Category response")
public record CategoryResponse(
        @Schema(description = "Category identifier", example = "46b6da6b-a10b-4a2f-95ba-a0f2bc43e301")
        UUID id,

        @Schema(description = "Category name", example = "Salary")
        String name,

        @Schema(description = "Category type", example = "INCOME", allowableValues = {"INCOME", "EXPENSE"})
        CategoryType type,

        @Schema(description = "Whether this is a system-defined category", example = "false")
        boolean isSystem,

        @Schema(description = "Identifier of the user who created the category", example = "f8bd6314-3ecc-4fd3-bf45-f8be61ad2a36")
        UUID createdBy,

        @Schema(description = "Creation timestamp (UTC)", example = "2026-04-06T08:00:00Z")
        Instant createdAt
) {}
