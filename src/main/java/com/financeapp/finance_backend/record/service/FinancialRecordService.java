package com.financeapp.finance_backend.record.service;

import com.financeapp.finance_backend.record.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FinancialRecordService {
    RecordResponse create(CreateRecordRequest request, UUID currentUserId, String ipAddress);
    RecordResponse findById(UUID id);
    Page<RecordResponse> findAll(RecordFilterCriteria criteria, Pageable pageable);
    RecordResponse update(UUID id, UpdateRecordRequest request, UUID currentUserId, String ipAddress);
    RecordResponse softDelete(UUID id, UUID currentUserId, String ipAddress);
}
