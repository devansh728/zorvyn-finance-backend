package com.financeapp.finance_backend.analytics;

import com.financeapp.finance_backend.analytics.dto.*;
import com.financeapp.finance_backend.analytics.service.AnalyticsServiceImpl;
import com.financeapp.finance_backend.record.entity.TransactionType;
import com.financeapp.finance_backend.record.repository.FinancialRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock FinancialRecordRepository recordRepository;

    @InjectMocks
    AnalyticsServiceImpl analyticsService;

    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        start = Instant.now().minusSeconds(86400L * 30);
        end = Instant.now();
    }

    // ===== SUMMARY =====

    @Test
    @DisplayName("getSummary: no data → returns all zeros")
    void getSummary_noData_returnsZeros() {
        when(recordRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(recordRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(recordRepository.countByDateRange(any(), any())).thenReturn(0L);

        SummaryResponse summary = analyticsService.getSummary(start, end);

        assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.recordCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getSummary: mixed data → correct totals and net")
    void getSummary_mixedData_returnsCorrectTotals() {
        when(recordRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("5000.00"));
        when(recordRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("2000.00"));
        when(recordRepository.countByDateRange(any(), any())).thenReturn(5L);

        SummaryResponse summary = analyticsService.getSummary(start, end);

        assertThat(summary.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(summary.totalExpense()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(summary.netBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(summary.recordCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getSummary: null params → uses defaults without error")
    void getSummary_nullParams_usesDefaults() {
        when(recordRepository.sumByTypeAndDateRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(recordRepository.countByDateRange(any(), any())).thenReturn(0L);

        assertThatNoException().isThrownBy(() -> analyticsService.getSummary(null, null));
    }

    // ===== CATEGORY BREAKDOWN =====

    @Test
    @DisplayName("getCategoryBreakdown: two categories → percentages sum to 100%")
    void getCategoryBreakdown_twoCategories_percentagesSumTo100() {
        UUID catA = UUID.randomUUID();
        UUID catB = UUID.randomUUID();

        // Use Object[] explicitly with the exact types our code expects
        Object[] rowA = new Object[]{catA, "Salary",    new BigDecimal("3000.00"), 2L};
        Object[] rowB = new Object[]{catB, "Freelance", new BigDecimal("1000.00"), 1L};

        when(recordRepository.categoryBreakdown(any(), any(), any()))
                .thenReturn(List.of(rowA, rowB));

        List<CategoryBreakdownResponse> result =
                analyticsService.getCategoryBreakdown("INCOME", start, end);

        assertThat(result).hasSize(2);
        BigDecimal totalPct = result.stream()
                .map(CategoryBreakdownResponse::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPct).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("getCategoryBreakdown: no data → empty list")
    void getCategoryBreakdown_noData_returnsEmpty() {
        when(recordRepository.categoryBreakdown(any(), any(), any())).thenReturn(List.of());

        var result = analyticsService.getCategoryBreakdown("INCOME", start, end);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryBreakdown: single category → 100% percentage")
    void getCategoryBreakdown_singleCategory_shows100Percent() {
        UUID catId = UUID.randomUUID();
        Object[] row = new Object[]{catId, "Salary", new BigDecimal("5000.00"), 3L};

        when(recordRepository.categoryBreakdown(any(), any(), any()))
                .thenReturn(List.<Object[]>of(row));

        var result = analyticsService.getCategoryBreakdown("INCOME", start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).percentage()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // ===== TRENDS =====

    @Test
    @DisplayName("getTrends: MONTHLY → calls monthlyTrends only")
    void getTrends_monthly_callsMonthlyQuery() {
        when(recordRepository.monthlyTrends(any(), any())).thenReturn(List.of());

        analyticsService.getTrends(TrendInterval.MONTHLY, start, end);

        verify(recordRepository).monthlyTrends(any(), any());
        verify(recordRepository, never()).dailyTrends(any(), any());
        verify(recordRepository, never()).weeklyTrends(any(), any());
    }

    @Test
    @DisplayName("getTrends: DAILY → calls dailyTrends only")
    void getTrends_daily_callsDailyQuery() {
        when(recordRepository.dailyTrends(any(), any())).thenReturn(List.of());

        analyticsService.getTrends(TrendInterval.DAILY, start, end);

        verify(recordRepository).dailyTrends(any(), any());
        verify(recordRepository, never()).monthlyTrends(any(), any());
    }

    @Test
    @DisplayName("getTrends: WEEKLY → calls weeklyTrends only")
    void getTrends_weekly_callsWeeklyQuery() {
        when(recordRepository.weeklyTrends(any(), any())).thenReturn(List.of());

        analyticsService.getTrends(TrendInterval.WEEKLY, start, end);

        verify(recordRepository).weeklyTrends(any(), any());
        verify(recordRepository, never()).monthlyTrends(any(), any());
    }
}
