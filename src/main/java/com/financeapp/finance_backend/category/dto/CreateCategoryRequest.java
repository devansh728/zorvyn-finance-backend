package com.financeapp.finance_backend.category.dto;

import com.financeapp.finance_backend.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull CategoryType type
) {}
