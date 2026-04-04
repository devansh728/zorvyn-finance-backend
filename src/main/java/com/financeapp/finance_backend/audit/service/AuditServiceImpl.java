package com.financeapp.finance_backend.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.finance_backend.audit.entity.AuditLog;
import com.financeapp.finance_backend.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void log(String entityType, UUID entityId, String action, UUID performedBy,
                    Object oldValues, Object newValues, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(performedBy)
                    .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : null)
                    .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : null)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log values for entityType={}, entityId={}", entityType, entityId, e);
        }
    }

    @Override
    @Async
    public void log(String entityType, UUID entityId, String action, UUID performedBy) {
        log(entityType, entityId, action, performedBy, null, null, null);
    }
}
