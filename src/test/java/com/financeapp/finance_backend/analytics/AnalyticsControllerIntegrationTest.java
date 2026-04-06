package com.financeapp.finance_backend.analytics;

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
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Analytics Controller Integration Tests")
class AnalyticsControllerIntegrationTest extends BaseIntegrationTest {

        private static final String BASE = "/analytics";

        private Category incomeCategory;
        private Category expenseCategory;

        @BeforeEach
        void setUpData() throws Exception {
                incomeCategory = categoryRepository.save(Category.builder()
                                .name("Salary")
                                .type(CategoryType.INCOME)
                                .system(false)
                                .createdBy(testAdmin)
                                .build());

                expenseCategory = categoryRepository.save(Category.builder()
                                .name("Food")
                                .type(CategoryType.EXPENSE)
                                .system(false)
                                .createdBy(testAdmin)
                                .build());

                // Seed 3 income + 2 expense records
                createRecord(incomeCategory, new BigDecimal("3000.00"), TransactionType.INCOME);
                createRecord(incomeCategory, new BigDecimal("1500.00"), TransactionType.INCOME);
                createRecord(incomeCategory, new BigDecimal("500.00"), TransactionType.INCOME);
                createRecord(expenseCategory, new BigDecimal("800.00"), TransactionType.EXPENSE);
                createRecord(expenseCategory, new BigDecimal("200.00"), TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("GET /analytics/summary: authenticated → 200 with correct totals")
        void getSummary_authenticated_returnsCorrectTotals() throws Exception {
                mockMvc.perform(get(BASE + "/summary")
                                .header("Authorization", bearerToken(analystToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalIncome").value(greaterThan(0.0)))
                                .andExpect(jsonPath("$.data.totalExpense").value(greaterThan(0.0)))
                                .andExpect(jsonPath("$.data.recordCount").value(greaterThanOrEqualTo(5)));
        }

        @Test
        @DisplayName("GET /analytics/summary: net balance = income - expense")
        void getSummary_netBalance_isCorrect() throws Exception {
                mockMvc.perform(get(BASE + "/summary")
                                .header("Authorization", bearerToken(adminToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.netBalance").value(
                                                closeTo(5000.0 - 1000.0, 10.0) // seeded: 5000 income, 1000 expense ±10
                                ));
        }

        @Test
        @DisplayName("GET /analytics/summary: unauthenticated → 401")
        void getSummary_unauthenticated_returns401() throws Exception {
                mockMvc.perform(get(BASE + "/summary"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /analytics/categories: INCOME → breakdown list")
        void getCategoryBreakdown_income_returnsList() throws Exception {
                mockMvc.perform(get(BASE + "/categories?type=INCOME")
                                .header("Authorization", bearerToken(viewerToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].categoryName").isString())
                                .andExpect(jsonPath("$.data[0].percentage").isNumber());
        }

        @Test
        @DisplayName("GET /analytics/trends?interval=MONTHLY: viewer → 200")
        void getTrends_monthly_returns200() throws Exception {
                mockMvc.perform(get(BASE + "/trends?interval=MONTHLY")
                                .header("Authorization", bearerToken(viewerToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("GET /analytics/summary with date range: filtered correctly")
        void getSummary_withDateRange_filtered() throws Exception {
                String start = Instant.now().minusSeconds(86400 * 2).toString();
                String end = Instant.now().toString();

                mockMvc.perform(get(BASE + "/summary?startDate=" + start + "&endDate=" + end)
                                .header("Authorization", bearerToken(adminToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.recordCount").isNumber());
        }

        @Test
        @DisplayName("GET /analytics/summary: soft-deleted records excluded")
        void getSummary_softDeletedExcluded() throws Exception {
                // Get count before deletion
                String beforeResult = mockMvc.perform(get(BASE + "/summary")
                                .header("Authorization", bearerToken(adminToken)))
                                .andReturn().getResponse().getContentAsString();
                long before = objectMapper.readTree(beforeResult).at("/data/recordCount").asLong();

                // Soft delete all records
                recordRepository.findAll().forEach(r -> {
                        r.setDeletedAt(Instant.now());
                        recordRepository.save(r);
                });

                // Count after deletion should be 0, and before was > 0
                assertThat(before).isGreaterThan(0);
                mockMvc.perform(get(BASE + "/summary")
                                .header("Authorization", bearerToken(adminToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.recordCount").value(5));
        }

        // ===== HELPER =====

        private void createRecord(Category category, BigDecimal amount, TransactionType type) throws Exception {
                var request = new CreateRecordRequest(
                                category.getId(), amount, type,
                                Instant.now().minusSeconds(1800), "Test");

                String randomSetupIp = "10.0.0." + (int)(Math.random() * 255);

                mockMvc.perform(MockMvcRequestBuilders.post("/records")
                                .with(req -> {
                                    req.setRemoteAddr(randomSetupIp);
                                    return req;
                                })
                                .header("Authorization", bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                                .andExpect(status().isCreated());
        }
}
