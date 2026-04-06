package com.financeapp.finance_backend.record.controller;

import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.PagedMeta;
import com.financeapp.finance_backend.common.util.SecurityContextUtil;
import com.financeapp.finance_backend.record.dto.*;
import com.financeapp.finance_backend.record.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

        @Operation(
            summary = "List financial records",
            description = "Retrieves paged financial records with optional filters. Requires ANALYST or ADMIN role.")
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Records retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ANALYST role required", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> listRecords(
            @Parameter(description = "Filter criteria")
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

        @Operation(
            summary = "Get financial record by ID",
            description = "Returns one financial record by its identifier. Requires ANALYST or ADMIN role.")
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Record retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ANALYST role required", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
        public ResponseEntity<ApiResponse<RecordResponse>> getById(
            @Parameter(description = "Record identifier", example = "f32dd26f-7c66-43e8-9162-dd26f080829b")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recordService.findById(id), "Record retrieved"));
    }

        @Operation(
            summary = "Create financial record",
            description = "Creates a new financial record. Requires ADMIN role.")
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Record created", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
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

        @Operation(
            summary = "Update financial record",
            description = "Partially updates a financial record using optimistic locking. Requires ADMIN role.")
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Record updated", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
            @Parameter(description = "Record identifier", example = "f32dd26f-7c66-43e8-9162-dd26f080829b")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        RecordResponse updated = recordService.update(id, request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(updated, "Record updated"));
    }

        @Operation(
            summary = "Soft delete financial record",
            description = "Marks a record as deleted without physically removing it. Requires ADMIN role.")
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Record deleted", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecordResponse>> deleteRecord(
            @Parameter(description = "Record identifier", example = "f32dd26f-7c66-43e8-9162-dd26f080829b")
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        RecordResponse deleted = recordService.softDelete(id, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(deleted, "Record deleted"));
    }
}
