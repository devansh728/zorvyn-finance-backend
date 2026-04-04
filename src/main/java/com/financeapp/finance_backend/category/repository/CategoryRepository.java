package com.financeapp.finance_backend.category.repository;

import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTypeOrderByNameAsc(CategoryType type);

    List<Category> findAllByOrderByTypeAscNameAsc();

    boolean existsByNameIgnoreCaseAndTypeAndSystemTrue(String name, CategoryType type);

    boolean existsByNameIgnoreCaseAndTypeAndCreatedByIdAndSystemFalse(String name, CategoryType type, UUID createdById);

    @Query(value = "SELECT COUNT(*) FROM financial_records WHERE category_id = :categoryId", nativeQuery = true)
    long countAllRecordsByCategoryIncludingDeleted(UUID categoryId);
}
