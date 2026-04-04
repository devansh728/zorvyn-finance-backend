package com.financeapp.finance_backend.record.repository;

import com.financeapp.finance_backend.record.entity.FinancialRecord;
import com.financeapp.finance_backend.record.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, UUID>,
        JpaSpecificationExecutor<FinancialRecord> {

    // ---- Analytics aggregation queries ----

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
           "WHERE r.type = :type AND r.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDateRange(TransactionType type, Instant start, Instant end);

    @Query("SELECT COUNT(r) FROM FinancialRecord r " +
           "WHERE r.transactionDate BETWEEN :start AND :end")
    long countByDateRange(Instant start, Instant end);

    @Query("SELECT r.category.id, r.category.name, COALESCE(SUM(r.amount), 0), COUNT(r) " +
           "FROM FinancialRecord r " +
           "WHERE r.type = :type AND r.transactionDate BETWEEN :start AND :end " +
           "GROUP BY r.category.id, r.category.name " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> categoryBreakdown(TransactionType type, Instant start, Instant end);

    @Query(value = "SELECT DATE_TRUNC('day', transaction_date) as period, " +
                   "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) as income, " +
                   "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) as expense " +
                   "FROM financial_records " +
                   "WHERE deleted_at IS NULL AND transaction_date BETWEEN :start AND :end " +
                   "GROUP BY DATE_TRUNC('day', transaction_date) " +
                   "ORDER BY period ASC",
           nativeQuery = true)
    List<Object[]> dailyTrends(Instant start, Instant end);

    @Query(value = "SELECT DATE_TRUNC('week', transaction_date) as period, " +
                   "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) as income, " +
                   "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) as expense " +
                   "FROM financial_records " +
                   "WHERE deleted_at IS NULL AND transaction_date BETWEEN :start AND :end " +
                   "GROUP BY DATE_TRUNC('week', transaction_date) " +
                   "ORDER BY period ASC",
           nativeQuery = true)
    List<Object[]> weeklyTrends(Instant start, Instant end);

    @Query(value = "SELECT DATE_TRUNC('month', transaction_date) as period, " +
                   "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) as income, " +
                   "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) as expense " +
                   "FROM financial_records " +
                   "WHERE deleted_at IS NULL AND transaction_date BETWEEN :start AND :end " +
                   "GROUP BY DATE_TRUNC('month', transaction_date) " +
                   "ORDER BY period ASC",
           nativeQuery = true)
    List<Object[]> monthlyTrends(Instant start, Instant end);
}
