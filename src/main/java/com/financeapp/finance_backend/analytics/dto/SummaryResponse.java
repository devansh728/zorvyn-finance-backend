package com.financeapp.finance_backend.analytics.dto;

import java.math.BigDecimal;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        long recordCount
) {}
