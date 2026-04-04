package com.financeapp.finance_backend.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityType,
        UUID entityId,
        String action,
        UUID performedBy,
        String oldValues,
        String newValues,
        String ipAddress,
        Instant createdAt
) {}
