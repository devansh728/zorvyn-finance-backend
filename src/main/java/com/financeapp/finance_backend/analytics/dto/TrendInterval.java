package com.financeapp.finance_backend.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Grouping interval used in trend analytics", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
public enum TrendInterval {
    DAILY, WEEKLY, MONTHLY
}
