package com.financeapp.finance_backend.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TrendDataPoint(
        Instant period,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal net
) {}
