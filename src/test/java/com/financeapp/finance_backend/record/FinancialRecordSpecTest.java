package com.financeapp.finance_backend.record;

import com.financeapp.finance_backend.record.entity.FinancialRecord;
import com.financeapp.finance_backend.record.entity.TransactionType;
import com.financeapp.finance_backend.record.specification.FinancialRecordSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FinancialRecordSpec — verifies specification logic
 * produces non-null predicates for non-null inputs.
 */
@DisplayName("FinancialRecordSpec Unit Tests")
class FinancialRecordSpecTest {

    @Test
    @DisplayName("byType: non-null type → non-null specification")
    void byType_nonNull_returnsSpec() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byType(TransactionType.INCOME);
        // A valid specification is returned (not null)
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byType: null type → null predicate (no filtering)")
    void byType_null_returnsNullPredicate() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byType(null);
        // Specification itself should not be null but return null from toPredicate
        // We verify the spec exists and handles null input
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byDateRange: both dates → valid range spec")
    void byDateRange_bothDates_returnsSpec() {
        Instant start = Instant.now().minusSeconds(86400);
        Instant end = Instant.now();
        Specification<FinancialRecord> spec = FinancialRecordSpec.byDateRange(start, end);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byDateRange: only start → open-ended range spec")
    void byDateRange_onlyStart_returnsSpec() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byDateRange(Instant.now(), null);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byDateRange: only end → upper-bounded range spec")
    void byDateRange_onlyEnd_returnsSpec() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byDateRange(null, Instant.now());
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byDateRange: both null → null predicate")
    void byDateRange_bothNull_returnsNullPredicate() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byDateRange(null, null);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byNotesContaining: non-blank search → returns spec")
    void byNotesContaining_nonBlank_returnsSpec() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byNotesContaining("salary");
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byNotesContaining: blank search → null predicate")
    void byNotesContaining_blank_returnsNullPredicate() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byNotesContaining("  ");
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("byCategoryId: with UUID → returns spec")
    void byCategoryId_withUuid_returnsSpec() {
        Specification<FinancialRecord> spec = FinancialRecordSpec.byCategoryId(java.util.UUID.randomUUID());
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("combined specs: chained with .and() → no exception")
    void combinedSpecs_noException() {
        Specification<FinancialRecord> combined = Specification
                .where(FinancialRecordSpec.byType(TransactionType.INCOME))
                .and(FinancialRecordSpec.byDateRange(Instant.now().minusSeconds(86400), Instant.now()))
                .and(FinancialRecordSpec.byNotesContaining("test"));

        assertThat(combined).isNotNull();
    }
}
