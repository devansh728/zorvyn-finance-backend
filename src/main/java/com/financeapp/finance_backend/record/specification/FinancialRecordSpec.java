package com.financeapp.finance_backend.record.specification;

import com.financeapp.finance_backend.record.entity.FinancialRecord;
import com.financeapp.finance_backend.record.entity.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public class FinancialRecordSpec {

    private FinancialRecordSpec() {}

    public static Specification<FinancialRecord> byType(TransactionType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<FinancialRecord> byCategoryId(UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<FinancialRecord> byDateRange(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get("transactionDate"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
            } else if (endDate != null) {
                return cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
            }
            return null;
        };
    }

    public static Specification<FinancialRecord> byNotesContaining(String search) {
        return (root, query, cb) ->
                (search == null || search.isBlank()) ? null
                        : cb.like(cb.lower(root.get("notes")), "%" + search.toLowerCase() + "%");
    }
}
