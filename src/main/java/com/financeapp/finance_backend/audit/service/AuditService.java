package com.financeapp.finance_backend.audit.service;

import java.util.UUID;

public interface AuditService {

    void log(String entityType, UUID entityId, String action, UUID performedBy,
             Object oldValues, Object newValues, String ipAddress);

    void log(String entityType, UUID entityId, String action, UUID performedBy);
}
