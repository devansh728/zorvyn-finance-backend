package com.financeapp.finance_backend.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Audit log entry response")
public record AuditLogResponse(
        @Schema(description = "Audit log identifier", example = "a0ea4d35-f35f-4bcb-951d-c286ed09081d")
        UUID id,

        @Schema(description = "Affected entity type", example = "RECORD")
        String entityType,

        @Schema(description = "Affected entity identifier", example = "f32dd26f-7c66-43e8-9162-dd26f080829b")
        UUID entityId,

        @Schema(description = "Action performed", example = "UPDATE")
        String action,

        @Schema(description = "User who performed the action", example = "f8bd6314-3ecc-4fd3-bf45-f8be61ad2a36")
        UUID performedBy,

        @Schema(description = "Serialized previous values", example = "{\"amount\": 100.00}")
        String oldValues,

        @Schema(description = "Serialized new values", example = "{\"amount\": 120.00}")
        String newValues,

        @Schema(description = "Client IP address", example = "192.168.1.44")
        String ipAddress,

        @Schema(description = "Action timestamp (UTC)", example = "2026-04-06T11:01:00Z")
        Instant createdAt
) {}
