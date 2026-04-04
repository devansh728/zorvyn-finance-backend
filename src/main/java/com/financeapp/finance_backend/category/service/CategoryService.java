package com.financeapp.finance_backend.category.service;

import com.financeapp.finance_backend.category.dto.*;
import com.financeapp.finance_backend.category.entity.CategoryType;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryResponse> listCategories(CategoryType type);
    CategoryResponse createCategory(CreateCategoryRequest request, UUID currentUserId, String ipAddress);
    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request, UUID currentUserId, String ipAddress);
    void deleteCategory(UUID id, UUID currentUserId, String ipAddress);
}
