package com.financeapp.finance_backend.category;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.category.dto.CreateCategoryRequest;
import com.financeapp.finance_backend.category.dto.UpdateCategoryRequest;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Category Controller Integration Tests")
class CategoryControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/categories";
    private Category testCategory;

    @BeforeEach
    void setUpCategory() {
        testCategory = categoryRepository.save(Category.builder()
                .name("Freelance Income")
                .type(CategoryType.INCOME)
                .system(false)
                .createdBy(testAdmin)
                .build());
    }

    @Test
    @DisplayName("GET /categories: authenticated user → 200 with list")
    void listCategories_authenticated_returns200() throws Exception {
        mockMvc.perform(get(BASE)
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /categories: unauthenticated → 401")
    void listCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /categories?type=INCOME: filtered → only INCOME categories")
    void listCategories_filterByType_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE + "?type=INCOME")
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].type", everyItem(is("INCOME"))));
    }

    @Test
    @DisplayName("POST /categories: ADMIN → 201 Created")
    void createCategory_asAdmin_returns201() throws Exception {
        var request = new CreateCategoryRequest("New Category", CategoryType.EXPENSE);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Category"))
                .andExpect(jsonPath("$.data.type").value("EXPENSE"))
                .andExpect(jsonPath("$.data.isSystem").value(false));
    }

    @Test
    @DisplayName("POST /categories: VIEWER → 403 Forbidden")
    void createCategory_asViewer_returns403() throws Exception {
        var request = new CreateCategoryRequest("Blocked", CategoryType.EXPENSE);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /categories: duplicate name (case-insensitive) → 409")
    void createCategory_duplicateName_returns409() throws Exception {
        var request = new CreateCategoryRequest("freelance income", CategoryType.INCOME);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /categories/{id}: ADMIN rename → 200")
    void updateCategory_asAdmin_returns200() throws Exception {
        var request = new UpdateCategoryRequest("Renamed Category");

        mockMvc.perform(patch(BASE + "/" + testCategory.getId())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Category"));
    }

    @Test
    @DisplayName("DELETE /categories/{id}: no records → 200 OK")
    void deleteCategory_noRecords_returns200() throws Exception {
        mockMvc.perform(delete(BASE + "/" + testCategory.getId())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /categories/{id}: ANALYST → 403")
    void deleteCategory_asAnalyst_returns403() throws Exception {
        mockMvc.perform(delete(BASE + "/" + testCategory.getId())
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }
}
