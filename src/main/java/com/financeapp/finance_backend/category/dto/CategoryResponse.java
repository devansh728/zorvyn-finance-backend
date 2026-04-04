package com.financeapp.finance_backend.category.dto;

import com.financeapp.finance_backend.category.entity.CategoryType;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        boolean isSystem,
        UUID createdBy,
        Instant createdAt
) {}
