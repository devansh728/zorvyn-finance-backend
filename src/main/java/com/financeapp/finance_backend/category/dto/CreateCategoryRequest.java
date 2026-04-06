package com.financeapp.finance_backend.category.dto;

import com.financeapp.finance_backend.category.entity.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to create a transaction category")
public record CreateCategoryRequest(
        @Schema(description = "Category display name", example = "Groceries")
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "Category type", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
        @NotNull CategoryType type
) {}
