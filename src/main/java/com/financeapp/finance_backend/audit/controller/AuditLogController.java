package com.financeapp.finance_backend.audit.controller;

import com.financeapp.finance_backend.audit.dto.AuditLogResponse;
import com.financeapp.finance_backend.audit.entity.AuditLog;
import com.financeapp.finance_backend.audit.repository.AuditLogRepository;
import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.PagedMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Audit Logs", description = "Audit log queries (admin only)")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @Operation(summary = "Get audit logs for an entity")
    @GetMapping("/entity/{type}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntity(
            @PathVariable String type,
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityId(type.toUpperCase(), id, pageable);
        List<AuditLogResponse> items = page.getContent().stream().map(this::toResponse).toList();

        PagedMeta meta = PagedMeta.builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.paged(items, meta, "Audit logs retrieved"));
    }

    @Operation(summary = "Get audit logs by performer")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByPerformer(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.findByPerformedBy(userId, pageable);
        List<AuditLogResponse> items = page.getContent().stream().map(this::toResponse).toList();

        PagedMeta meta = PagedMeta.builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.paged(items, meta, "Audit logs retrieved"));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getEntityType(), log.getEntityId(),
                log.getAction(), log.getPerformedBy(),
                log.getOldValues(), log.getNewValues(),
                log.getIpAddress(), log.getCreatedAt()
        );
    }
}
