package com.financeapp.finance_backend.category.service;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.category.dto.*;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.category.repository.CategoryRepository;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(CategoryType type) {
        List<Category> categories = (type != null)
                ? categoryRepository.findByTypeOrderByNameAsc(type)
                : categoryRepository.findAllByOrderByTypeAscNameAsc();

        return categories.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, UUID currentUserId, String ipAddress) {
        // Case-insensitive duplicate check for system categories
        if (categoryRepository.existsByNameIgnoreCaseAndTypeAndSystemTrue(request.name(), request.type())) {
            throw new DuplicateResourceException("Category", "name", request.name());
        }
        // And user-specific duplicate check
        if (categoryRepository.existsByNameIgnoreCaseAndTypeAndCreatedByIdAndSystemFalse(
                request.name(), request.type(), currentUserId)) {
            throw new DuplicateResourceException("Category", "name", request.name());
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        Category category = Category.builder()
                .name(request.name())
                .type(request.type())
                .system(false)
                .createdBy(creator)
                .build();

        category = categoryRepository.save(category);
        CategoryResponse response = toResponse(category);
        auditService.log("CATEGORY", category.getId(), "CREATE", currentUserId, null, response, ipAddress);
        return response;
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request, UUID currentUserId, String ipAddress) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (category.isSystem()) {
            throw new BusinessRuleException("System categories cannot be modified");
        }

        CategoryResponse oldState = toResponse(category);
        category.setName(request.name());
        category = categoryRepository.save(category);

        CategoryResponse newState = toResponse(category);
        auditService.log("CATEGORY", category.getId(), "UPDATE", currentUserId, oldState, newState, ipAddress);
        return newState;
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id, UUID currentUserId, String ipAddress) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (category.isSystem()) {
            throw new BusinessRuleException("System categories cannot be deleted");
        }

        long recordCount = categoryRepository.countAllRecordsByCategoryIncludingDeleted(id);
        if (recordCount > 0) {
            throw new BusinessRuleException(
                    "Cannot delete category with " + recordCount + " associated records");
        }

        CategoryResponse oldState = toResponse(category);
        categoryRepository.delete(category);
        auditService.log("CATEGORY", id, "SOFT_DELETE", currentUserId, oldState, null, ipAddress);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getType(), c.isSystem(),
                c.getCreatedBy() != null ? c.getCreatedBy().getId() : null,
                c.getCreatedAt()
        );
    }
}
