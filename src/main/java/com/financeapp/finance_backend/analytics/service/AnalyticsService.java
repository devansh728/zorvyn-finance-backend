package com.financeapp.finance_backend.analytics.service;

import com.financeapp.finance_backend.analytics.dto.*;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {
    SummaryResponse getSummary(Instant startDate, Instant endDate);
    List<CategoryBreakdownResponse> getCategoryBreakdown(String type, Instant startDate, Instant endDate);
    List<TrendDataPoint> getTrends(TrendInterval interval, Instant startDate, Instant endDate);
}
