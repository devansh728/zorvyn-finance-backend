package com.financeapp.finance_backend.category;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.category.dto.CreateCategoryRequest;
import com.financeapp.finance_backend.category.dto.UpdateCategoryRequest;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.category.repository.CategoryRepository;
import com.financeapp.finance_backend.category.service.CategoryServiceImpl;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;

    @InjectMocks
    CategoryServiceImpl categoryService;

    private UUID adminId;
    private User adminUser;
    private Category userCategory;
    private Category systemCategory;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminUser = User.builder()
                .email("admin@test.com")
                .passwordHash("h")
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(adminUser, adminId);
        } catch (Exception e) { throw new RuntimeException(e); }

        userCategory = Category.builder()
                .name("Freelance")
                .type(CategoryType.INCOME)
                .system(false)
                .createdBy(adminUser)
                .build();
        UUID catId = UUID.randomUUID();
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(userCategory, catId);
        } catch (Exception e) { throw new RuntimeException(e); }

        systemCategory = Category.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .system(true)
                .build();
        UUID sysId = UUID.randomUUID();
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(systemCategory, sysId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ===== LIST =====

    @Test
    @DisplayName("listCategories: with type filter → returns only that type")
    void listCategories_withType_returnsFiltered() {
        when(categoryRepository.findByTypeOrderByNameAsc(CategoryType.INCOME))
                .thenReturn(List.of(userCategory));

        var result = categoryService.listCategories(CategoryType.INCOME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    @DisplayName("listCategories: no type filter → returns all")
    void listCategories_noType_returnsAll() {
        when(categoryRepository.findAllByOrderByTypeAscNameAsc())
                .thenReturn(List.of(userCategory, systemCategory));

        var result = categoryService.listCategories(null);
        assertThat(result).hasSize(2);
    }

    // ===== CREATE =====

    @Test
    @DisplayName("createCategory: new name → created successfully")
    void createCategory_newName_success() {
        var req = new CreateCategoryRequest("Bonus", CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndSystemTrue("Bonus", CategoryType.INCOME)).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndCreatedByIdAndSystemFalse("Bonus", CategoryType.INCOME, adminId)).thenReturn(false);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = categoryService.createCategory(req, adminId, "127.0.0.1");

        assertThat(result.name()).isEqualTo("Bonus");
        assertThat(result.type()).isEqualTo(CategoryType.INCOME);
        assertThat(result.isSystem()).isFalse();
    }

    @Test
    @DisplayName("createCategory: duplicate of system category → DuplicateResourceException")
    void createCategory_duplicatesSystemCategory_throws() {
        var req = new CreateCategoryRequest("Salary", CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndSystemTrue("Salary", CategoryType.INCOME)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req, adminId, "127.0.0.1"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"freelance", "FREELANCE", "FreeLance"})
    @DisplayName("createCategory: case-insensitive duplicate → DuplicateResourceException")
    void createCategory_caseInsensitiveDuplicate_throws(String name) {
        var req = new CreateCategoryRequest(name, CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndSystemTrue(name, CategoryType.INCOME)).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndCreatedByIdAndSystemFalse(name, CategoryType.INCOME, adminId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req, adminId, "127.0.0.1"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ===== UPDATE =====

    @Test
    @DisplayName("updateCategory: valid rename → succeeds")
    void updateCategory_validRename_success() {
        when(categoryRepository.findById(userCategory.getId())).thenReturn(Optional.of(userCategory));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = categoryService.updateCategory(
                userCategory.getId(), new UpdateCategoryRequest("Updated Name"), adminId, "127.0.0.1");

        assertThat(result.name()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("updateCategory: system category → BusinessRuleException")
    void updateCategory_systemCategory_throws() {
        when(categoryRepository.findById(systemCategory.getId())).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(
                systemCategory.getId(), new UpdateCategoryRequest("New"), adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("System categories");
    }

    // ===== DELETE =====

    @Test
    @DisplayName("deleteCategory: no records → deletes successfully")
    void deleteCategory_noRecords_success() {
        when(categoryRepository.findById(userCategory.getId())).thenReturn(Optional.of(userCategory));
        when(categoryRepository.countAllRecordsByCategoryIncludingDeleted(userCategory.getId())).thenReturn(0L);
        doNothing().when(categoryRepository).delete(userCategory);

        assertThatNoException().isThrownBy(() ->
                categoryService.deleteCategory(userCategory.getId(), adminId, "127.0.0.1"));
    }

    @Test
    @DisplayName("deleteCategory: records exist → BusinessRuleException")
    void deleteCategory_recordsExist_throws() {
        when(categoryRepository.findById(userCategory.getId())).thenReturn(Optional.of(userCategory));
        when(categoryRepository.countAllRecordsByCategoryIncludingDeleted(userCategory.getId())).thenReturn(5L);

        assertThatThrownBy(() -> categoryService.deleteCategory(userCategory.getId(), adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5 associated records");
    }

    @Test
    @DisplayName("deleteCategory: system category → BusinessRuleException")
    void deleteCategory_systemCategory_throws() {
        when(categoryRepository.findById(systemCategory.getId())).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(systemCategory.getId(), adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("System categories");
    }

    @Test
    @DisplayName("deleteCategory: unknown ID → ResourceNotFoundException")
    void deleteCategory_unknownId_throws() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(unknownId, adminId, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
