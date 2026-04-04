package com.financeapp.finance_backend.analytics.service;

import com.financeapp.finance_backend.analytics.dto.*;
import com.financeapp.finance_backend.record.entity.TransactionType;
import com.financeapp.finance_backend.record.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final FinancialRecordRepository recordRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsSummary", key = "#startDate + '_' + #endDate")
    public SummaryResponse getSummary(Instant startDate, Instant endDate) {
        Instant start = startDate != null ? startDate : Instant.EPOCH;
        Instant end = endDate != null ? endDate : Instant.now();

        BigDecimal totalIncome = recordRepository.sumByTypeAndDateRange(TransactionType.INCOME, start, end);
        BigDecimal totalExpense = recordRepository.sumByTypeAndDateRange(TransactionType.EXPENSE, start, end);
        long count = recordRepository.countByDateRange(start, end);

        BigDecimal income = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        BigDecimal expense = totalExpense != null ? totalExpense : BigDecimal.ZERO;

        return new SummaryResponse(income, expense, income.subtract(expense), count);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsCategoryBreakdown", key = "#type + '_' + #startDate + '_' + #endDate")
    public List<CategoryBreakdownResponse> getCategoryBreakdown(String type, Instant startDate, Instant endDate) {
        Instant start = startDate != null ? startDate : Instant.EPOCH;
        Instant end = endDate != null ? endDate : Instant.now();

        TransactionType transactionType = type != null ? TransactionType.valueOf(type.toUpperCase()) : TransactionType.EXPENSE;
        List<Object[]> rows = recordRepository.categoryBreakdown(transactionType, start, end);

        if (rows.isEmpty()) return Collections.emptyList();

        // Calculate grand total for percentage
        BigDecimal grandTotal = rows.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(row -> {
            UUID categoryId = (UUID) row[0];
            String categoryName = (String) row[1];
            BigDecimal total = (BigDecimal) row[2];
            long count = ((Number) row[3]).longValue();

            BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : total.multiply(BigDecimal.valueOf(100))
                           .divide(grandTotal, 2, RoundingMode.HALF_UP);

            return new CategoryBreakdownResponse(categoryId, categoryName, total, count, percentage);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendDataPoint> getTrends(TrendInterval interval, Instant startDate, Instant endDate) {
        Instant start = startDate != null ? startDate : Instant.EPOCH;
        Instant end = endDate != null ? endDate : Instant.now();

        List<Object[]> rows = switch (interval) {
            case DAILY -> recordRepository.dailyTrends(start, end);
            case WEEKLY -> recordRepository.weeklyTrends(start, end);
            case MONTHLY -> recordRepository.monthlyTrends(start, end);
        };

        return rows.stream().map(row -> {
            Instant period = ((Timestamp) row[0]).toInstant();
            BigDecimal income = (BigDecimal) row[1];
            BigDecimal expense = (BigDecimal) row[2];
            return new TrendDataPoint(period, income, expense, income.subtract(expense));
        }).toList();
    }
}
