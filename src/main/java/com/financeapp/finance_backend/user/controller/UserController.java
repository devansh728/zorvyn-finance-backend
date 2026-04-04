package com.financeapp.finance_backend.user.controller;

import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.dto.PagedMeta;
import com.financeapp.finance_backend.common.util.SecurityContextUtil;
import com.financeapp.finance_backend.user.dto.*;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.service.UserService;
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

@Tag(name = "Users", description = "User management")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(currentUserId), "Profile retrieved"));
    }

    @Operation(summary = "List all users (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserResponse> page = userService.listUsers(role, status, pageable);

        PagedMeta meta = PagedMeta.builder()
                .pageNumber(page.getNumber()).pageSize(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .hasNext(page.hasNext()).hasPrevious(page.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), meta, "Users retrieved"));
    }

    @Operation(summary = "Get user by ID (admin)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id), "User retrieved"));
    }

    @Operation(summary = "Create user (admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String ipAddress = httpRequest.getRemoteAddr();
        UserResponse created = userService.createUser(request, currentUserId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "User created successfully"));
    }

    @Operation(summary = "Update user status (admin)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        UserResponse updated = userService.updateStatus(id, request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(updated, "User status updated"));
    }

    @Operation(summary = "Update user role (admin)")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        UserResponse updated = userService.updateRole(id, request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(updated, "User role updated"));
    }
}
