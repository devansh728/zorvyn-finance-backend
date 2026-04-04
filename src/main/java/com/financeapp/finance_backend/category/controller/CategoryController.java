package com.financeapp.finance_backend.category.controller;

import com.financeapp.finance_backend.category.dto.*;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.category.service.CategoryService;
import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.common.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = "Transaction category management")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List categories (all authenticated users)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @RequestParam(required = false) CategoryType type) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.listCategories(type), "Categories retrieved"));
    }

    @Operation(summary = "Create a new category (admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        CategoryResponse created = categoryService.createCategory(request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Category created"));
    }

    @Operation(summary = "Update category name (admin only)")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        CategoryResponse updated = categoryService.updateCategory(id, request, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(updated, "Category updated"));
    }

    @Operation(summary = "Delete category (admin only, fails if records exist)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        categoryService.deleteCategory(id, currentUserId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }
}
