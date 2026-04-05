package com.financeapp.finance_backend.record;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.category.repository.CategoryRepository;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.CustomConcurrentModificationException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.record.dto.CreateRecordRequest;
import com.financeapp.finance_backend.record.dto.RecordFilterCriteria;
import com.financeapp.finance_backend.record.dto.UpdateRecordRequest;
import com.financeapp.finance_backend.record.entity.FinancialRecord;
import com.financeapp.finance_backend.record.entity.TransactionType;
import com.financeapp.finance_backend.record.repository.FinancialRecordRepository;
import com.financeapp.finance_backend.record.service.FinancialRecordServiceImpl;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialRecordService Unit Tests")
class FinancialRecordServiceTest {

    @Mock FinancialRecordRepository recordRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;

    @InjectMocks
    FinancialRecordServiceImpl recordService;

    private UUID adminId;
    private UUID categoryId;
    private UUID recordId;
    private User adminUser;
    private Category incomeCategory;
    private Category expenseCategory;
    private FinancialRecord record;

    @BeforeEach
    void setUp() throws Exception {
        adminId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        recordId = UUID.randomUUID();

        adminUser = User.builder()
                .email("admin@test.com")
                .passwordHash("h")
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(adminUser, User.class, adminId);

        incomeCategory = Category.builder()
                .name("Freelance")
                .type(CategoryType.INCOME)
                .system(false)
                .createdBy(adminUser)
                .build();
        setId(incomeCategory, Category.class, categoryId);

        expenseCategory = Category.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .system(false)
                .createdBy(adminUser)
                .build();
        setId(expenseCategory, Category.class, UUID.randomUUID());

        record = FinancialRecord.builder()
                .createdBy(adminUser)
                .category(incomeCategory)
                .amount(new BigDecimal("1500.00"))
                .type(TransactionType.INCOME)
                .transactionDate(Instant.now().minusSeconds(3600))
                .notes("Test income")
                .build();
        setId(record, FinancialRecord.class, recordId);
        setVersion(record, 0);
    }

    private void setId(Object obj, Class<?> clazz, UUID id) throws Exception {
        var f = clazz.getDeclaredField("id");
        f.setAccessible(true);
        f.set(obj, id);
    }

    private void setVersion(FinancialRecord r, int version) throws Exception {
        var f = FinancialRecord.class.getDeclaredField("version");
        f.setAccessible(true);
        f.setInt(r, version);
    }

    // ===== CREATE =====

    @Test
    @DisplayName("create: valid INCOME record with INCOME category → success")
    void create_validIncomeRecord_success() {
        var req = new CreateRecordRequest(
                categoryId, new BigDecimal("1000.00"), TransactionType.INCOME,
                Instant.now().minusSeconds(60), "Test");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(incomeCategory));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = recordService.create(req, adminId, "127.0.0.1");

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        verify(auditService).log(eq("FINANCIAL_RECORD"), any(), eq("CREATE"), eq(adminId), isNull(), any(), anyString());
    }

    @Test
    @DisplayName("create: INCOME record with EXPENSE category → BusinessRuleException")
    void create_typeMismatch_throws() {
        UUID expCatId = expenseCategory.getId();
        var req = new CreateRecordRequest(
                expCatId, new BigDecimal("500.00"), TransactionType.INCOME,
                Instant.now().minusSeconds(60), null);

        when(categoryRepository.findById(expCatId)).thenReturn(Optional.of(expenseCategory));

        assertThatThrownBy(() -> recordService.create(req, adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("create: unknown categoryId → ResourceNotFoundException")
    void create_unknownCategory_throws() {
        var req = new CreateRecordRequest(
                UUID.randomUUID(), new BigDecimal("100.00"), TransactionType.INCOME,
                Instant.now().minusSeconds(60), null);

        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.create(req, adminId, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== FIND BY ID =====

    @Test
    @DisplayName("findById: existing → returns record")
    void findById_existing_returnsRecord() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));

        var result = recordService.findById(recordId);

        assertThat(result.id()).isEqualTo(recordId);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("findById: not found → ResourceNotFoundException")
    void findById_notFound_throws() {
        when(recordRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== UPDATE =====

    @Test
    @DisplayName("update: correct version → partial update succeeds")
    void update_correctVersion_success() throws Exception {
        var req = new UpdateRecordRequest(null, new BigDecimal("2000.00"), null, "Updated notes", 0);

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = recordService.update(recordId, req, adminId, "127.0.0.1");

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.notes()).isEqualTo("Updated notes");
    }

    @Test
    @DisplayName("update: wrong version → CustomConcurrentModificationException")
    void update_wrongVersion_throws() {
        var req = new UpdateRecordRequest(null, null, null, null, 99); // wrong version

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record)); // record.version = 0

        assertThatThrownBy(() -> recordService.update(recordId, req, adminId, "127.0.0.1"))
                .isInstanceOf(CustomConcurrentModificationException.class)
                .hasMessageContaining("modified");
    }

    // ===== SOFT DELETE =====

    @Test
    @DisplayName("softDelete: sets deletedAt, audit logged")
    void softDelete_setsDeletedAt() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenReturn(record);

        var result = recordService.softDelete(recordId, adminId, "127.0.0.1");

        assertThat(result).isNotNull(); // returns old state
        verify(recordRepository).save(argThat(r -> r.getDeletedAt() != null));
        verify(auditService).log(eq("FINANCIAL_RECORD"), eq(recordId), eq("SOFT_DELETE"), eq(adminId), any(), isNull(), anyString());
    }

    // ===== FIND ALL =====

    @Test
    @DisplayName("findAll: with filter criteria → calls specification")
    void findAll_withCriteria_callsSpecification() {
        var criteria = new RecordFilterCriteria(TransactionType.INCOME, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        @SuppressWarnings("unchecked")
        Specification<FinancialRecord> anySpec = any(Specification.class);
        when(recordRepository.findAll(anySpec, eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var page = recordService.findAll(criteria, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
