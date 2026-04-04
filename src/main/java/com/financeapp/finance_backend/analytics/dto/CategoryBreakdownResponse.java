package com.financeapp.finance_backend.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBreakdownResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal total,
        long count,
        BigDecimal percentage
) {}
