package com.financeapp.finance_backend.record.service;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.category.dto.CategoryResponse;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.repository.CategoryRepository;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.CustomConcurrentModificationException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.record.dto.*;
import com.financeapp.finance_backend.record.entity.FinancialRecord;
import com.financeapp.finance_backend.record.repository.FinancialRecordRepository;
import com.financeapp.finance_backend.record.specification.FinancialRecordSpec;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "analyticsSummary", allEntries = true),
        @CacheEvict(cacheNames = "analyticsCategoryBreakdown", allEntries = true)
    })
    public RecordResponse create(CreateRecordRequest request, UUID currentUserId, String ipAddress) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        // Category type must match record type
        if (!category.getType().name().equals(request.type().name())) {
            throw new BusinessRuleException(
                    "Category type '" + category.getType() + "' does not match record type '" + request.type() + "'");
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        FinancialRecord record = FinancialRecord.builder()
                .createdBy(creator)
                .category(category)
                .amount(request.amount())
                .type(request.type())
                .transactionDate(request.transactionDate())
                .notes(request.notes())
                .build();

        record = recordRepository.save(record);
        RecordResponse response = toResponse(record);
        auditService.log("FINANCIAL_RECORD", record.getId(), "CREATE", currentUserId, null, response, ipAddress);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public RecordResponse findById(UUID id) {
        return recordRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponse> findAll(RecordFilterCriteria criteria, Pageable pageable) {
        Specification<FinancialRecord> spec = Specification
                .where(FinancialRecordSpec.byType(criteria.type()))
                .and(FinancialRecordSpec.byCategoryId(criteria.categoryId()))
                .and(FinancialRecordSpec.byDateRange(criteria.startDate(), criteria.endDate()))
                .and(FinancialRecordSpec.byNotesContaining(criteria.search()));

        return recordRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "analyticsSummary", allEntries = true),
        @CacheEvict(cacheNames = "analyticsCategoryBreakdown", allEntries = true)
    })
    public RecordResponse update(UUID id, UpdateRecordRequest request, UUID currentUserId, String ipAddress) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", "id", id));

        // Optimistic lock check
        if (record.getVersion() != request.version()) {
            throw new CustomConcurrentModificationException(
                    "Record was modified by another request. Please refresh and try again.");
        }

        RecordResponse oldState = toResponse(record);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));
            // Type consistency check
            if (!category.getType().name().equals(record.getType().name())) {
                throw new BusinessRuleException("Cannot change category to a different type");
            }
            record.setCategory(category);
        }

        if (request.amount() != null) record.setAmount(request.amount());
        if (request.transactionDate() != null) record.setTransactionDate(request.transactionDate());
        if (request.notes() != null) record.setNotes(request.notes());

        try {
            record = recordRepository.save(record);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new CustomConcurrentModificationException(
                    "Concurrent modification detected. Please refresh and try again.");
        }

        RecordResponse newState = toResponse(record);
        auditService.log("FINANCIAL_RECORD", record.getId(), "UPDATE", currentUserId, oldState, newState, ipAddress);
        return newState;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "analyticsSummary", allEntries = true),
        @CacheEvict(cacheNames = "analyticsCategoryBreakdown", allEntries = true)
    })
    public RecordResponse softDelete(UUID id, UUID currentUserId, String ipAddress) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", "id", id));

        RecordResponse oldState = toResponse(record);
        record.setDeletedAt(Instant.now());
        record = recordRepository.save(record);

        auditService.log("FINANCIAL_RECORD", record.getId(), "SOFT_DELETE", currentUserId, oldState, null, ipAddress);
        return oldState;
    }

    public RecordResponse toResponse(FinancialRecord r) {
        Category cat = r.getCategory();
        CategoryResponse catResponse = new CategoryResponse(
                cat.getId(), cat.getName(), cat.getType(), cat.isSystem(),
                cat.getCreatedBy() != null ? cat.getCreatedBy().getId() : null,
                cat.getCreatedAt()
        );

        return new RecordResponse(
                r.getId(), r.getCreatedBy().getId(), catResponse,
                r.getAmount(), r.getType(), r.getTransactionDate(),
                r.getNotes(), r.getCreatedAt(), r.getUpdatedAt(), r.getVersion()
        );
    }
}
