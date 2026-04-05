package com.financeapp.finance_backend.audit;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.record.dto.CreateRecordRequest;
import com.financeapp.finance_backend.record.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Audit Log Integration Tests")
class AuditLogControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/audit-logs";

    private Category testCategory;

    @BeforeEach
    void setUpCategory() {
        testCategory = categoryRepository.save(Category.builder()
                .name("Audit Category")
                .type(CategoryType.INCOME)
                .system(false)
                .createdBy(testAdmin)
                .build());
    }

    @Test
    @DisplayName("Create record → audit log CREATE entry created")
    void createRecord_createsAuditLog() throws Exception {
        long beforeCount = auditLogRepository.count();

        var request = new CreateRecordRequest(
                testCategory.getId(), new BigDecimal("1000.00"),
                TransactionType.INCOME, Instant.now().minusSeconds(60), "test");

        mockMvc.perform(MockMvcRequestBuilders.post("/records")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        // Allow async audit write to complete
        Thread.sleep(500);

        long afterCount = auditLogRepository.count();
        assertThat(afterCount).isGreaterThan(beforeCount);
    }

    @Test
    @DisplayName("GET /audit-logs/entity: ADMIN → 200 with paginated results")
    void getAuditLogsByEntity_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.pageNumber").isNumber());
    }

    @Test
    @DisplayName("GET /audit-logs/entity: VIEWER → 403 Forbidden")
    void getAuditLogsByEntity_asViewer_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit-logs/entity: ANALYST → 403 Forbidden")
    void getAuditLogsByEntity_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/entity/USER/" + testAdmin.getId())
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit-logs/user: ADMIN → 200")
    void getAuditLogsByUser_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/user/" + testAdmin.getId())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /audit-logs: ADMIN, unknown entity ID → 200 empty list")
    void getAuditLogs_unknownEntity_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE + "/entity/USER/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
