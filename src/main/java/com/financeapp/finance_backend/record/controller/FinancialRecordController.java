package com.financeapp.finance_backend.record.controller;

import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.PagedMeta;
import com.financeapp.finance_backend.common.util.SecurityContextUtil;
import com.financeapp.finance_backend.record.dto.*;
import com.financeapp.finance_backend.record.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Financial Records", description = "Financial record CRUD with filtering and pagination")
@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @Operation(summary = "List records with filters (analyst, admin)")
    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> listRecords(
            RecordFilterCriteria criteria,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RecordResponse> page = recordService.findAll(criteria, pageable);

        PagedMeta meta = PagedMeta.builder()
                .pageNumber(page.getNumber()).pageSize(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .hasNext(page.hasNext()).hasPrevious(page.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), meta, "Records retrieved"));
    }

    @Operation(summary = "Get record by ID (analyst, admin)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<ApiResponse<RecordResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recordService.findById(id), "Record retrieved"));
    }

    @Operation(summary = "Create financial record (admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecordResponse>> createRecord(
            @Valid @RequestBody CreateRecordRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        RecordResponse created = recordService.create(request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Record created"));
    }

    @Operation(summary = "Update financial record (admin only, requires version)")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        RecordResponse updated = recordService.update(id, request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(updated, "Record updated"));
    }

    @Operation(summary = "Soft delete financial record (admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecordResponse>> deleteRecord(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        RecordResponse deleted = recordService.softDelete(id, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(deleted, "Record deleted"));
    }
}
