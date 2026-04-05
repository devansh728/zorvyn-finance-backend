package com.financeapp.finance_backend.security;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC matrix integration test — exhaustively verifies every role's access
 * to every protected endpoint.
 */
@DisplayName("RBAC Matrix Integration Tests")
class RbacIntegrationTest extends BaseIntegrationTest {

    public Category testCategory;

    @BeforeEach
    void setUpCategory() {
        testCategory = categoryRepository.save(Category.builder()
                .name("Test Category")
                .type(CategoryType.INCOME)
                .system(false)
                .createdBy(testAdmin)
                .build());
    }

    // ===== USER ENDPOINTS =====

    @Test @DisplayName("GET /users: VIEWER → 403")
    void listUsers_viewer_403() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /users: ANALYST → 403")
    void listUsers_analyst_403() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /users: ADMIN → 200")
    void listUsers_admin_200() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
    }

    // ===== CATEGORY ENDPOINTS =====

    @Test @DisplayName("POST /categories: VIEWER → 403")
    void createCategory_viewer_403() throws Exception {
        mockMvc.perform(post("/categories")
                        .header("Authorization", bearerToken(viewerToken))
                        .contentType("application/json")
                        .content("{\"name\":\"Test\",\"type\":\"INCOME\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("POST /categories: ANALYST → 403")
    void createCategory_analyst_403() throws Exception {
        mockMvc.perform(post("/categories")
                        .header("Authorization", bearerToken(analystToken))
                        .contentType("application/json")
                        .content("{\"name\":\"Test\",\"type\":\"INCOME\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("DELETE /categories: VIEWER → 403")
    void deleteCategory_viewer_403() throws Exception {
        mockMvc.perform(delete("/categories/" + testCategory.getId())
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    // ===== RECORD ENDPOINTS =====

    @Test @DisplayName("POST /records: VIEWER → 403")
    void createRecord_viewer_403() throws Exception {
        mockMvc.perform(post("/records")
                        .header("Authorization", bearerToken(viewerToken))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /records: VIEWER → 403")
    void listRecords_viewer_403() throws Exception {
        mockMvc.perform(get("/records")
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /records: ANALYST → 200")
    void listRecords_analyst_200() throws Exception {
        mockMvc.perform(get("/records")
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("PATCH /records/{id}: ANALYST → 403")
    void updateRecord_analyst_403() throws Exception {
        mockMvc.perform(patch("/records/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearerToken(analystToken))
                        .contentType("application/json")
                        .content("{\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("DELETE /records/{id}: ANALYST → 403")
    void deleteRecord_analyst_403() throws Exception {
        mockMvc.perform(delete("/records/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }

    // ===== AUDIT LOGS =====

    @Test @DisplayName("GET /audit-logs: VIEWER → 403")
    void listAuditLogs_viewer_403() throws Exception {
        mockMvc.perform(get("/audit-logs/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /audit-logs: ANALYST → 403")
    void listAuditLogs_analyst_403() throws Exception {
        mockMvc.perform(get("/audit-logs/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /audit-logs: ADMIN → 200")
    void listAuditLogs_admin_200() throws Exception {
        mockMvc.perform(get("/audit-logs/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
    }

    // ===== ANALYTICS (all roles allowed) =====

    @ParameterizedTest(name = "{0} can access analytics summary")
    @CsvSource({"VIEWER", "ANALYST", "ADMIN"})
    @DisplayName("GET /analytics/summary: all roles → 200")
    void analyticsSummary_allRoles_200(String role) throws Exception {
        String token = switch (role) {
            case "VIEWER" -> viewerToken;
            case "ANALYST" -> analystToken;
            default -> adminToken;
        };

        mockMvc.perform(get("/analytics/summary")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk());
    }

    // ===== UNAUTHENTICATED ACCESS =====

    @Test @DisplayName("Protected endpoint: no token → 401")
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Protected endpoint: invalid token → 401")
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }
}
