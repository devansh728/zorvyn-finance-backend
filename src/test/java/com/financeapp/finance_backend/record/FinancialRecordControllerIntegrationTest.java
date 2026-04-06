package com.financeapp.finance_backend.record;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.category.entity.Category;
import com.financeapp.finance_backend.category.entity.CategoryType;
import com.financeapp.finance_backend.record.dto.CreateRecordRequest;
import com.financeapp.finance_backend.record.dto.UpdateRecordRequest;
import com.financeapp.finance_backend.record.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Financial Record Controller Integration Tests")
class FinancialRecordControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/records";

    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUpCategories() {
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
    }

    // ===== CREATE =====

    @Test
    @DisplayName("POST /records: ADMIN with valid data → 201")
    void createRecord_asAdmin_returns201() throws Exception {
        var request = new CreateRecordRequest(
                incomeCategory.getId(),
                new BigDecimal("2500.00"),
                TransactionType.INCOME,
                Instant.now().minusSeconds(3600),
                "Monthly salary");

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(2500.00))
                .andExpect(jsonPath("$.data.type").value("INCOME"))
                .andExpect(jsonPath("$.data.version").isNumber());
    }

    @Test
    @DisplayName("POST /records: ANALYST → 403 Forbidden")
    void createRecord_asAnalyst_returns403() throws Exception {
        var request = new CreateRecordRequest(
                incomeCategory.getId(), new BigDecimal("100.00"),
                TransactionType.INCOME, Instant.now().minusSeconds(60), null);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(analystToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /records: amount = 0 → 422 Validation Error")
    void createRecord_zeroAmount_returns422() throws Exception {
        var request = new CreateRecordRequest(
                incomeCategory.getId(), BigDecimal.ZERO,
                TransactionType.INCOME, Instant.now().minusSeconds(60), null);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("POST /records: future date → 422 Validation Error")
    void createRecord_futureDate_returns422() throws Exception {
        var request = new CreateRecordRequest(
                incomeCategory.getId(), new BigDecimal("100.00"),
                TransactionType.INCOME, Instant.now().plusSeconds(86400), null);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("POST /records: INCOME type with EXPENSE category → 422 type mismatch")
    void createRecord_typeMismatch_returns422() throws Exception {
        var request = new CreateRecordRequest(
                expenseCategory.getId(), new BigDecimal("500.00"),
                TransactionType.INCOME, Instant.now().minusSeconds(60), null);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== READ =====

    @Test
    @DisplayName("GET /records: VIEWER → 403 Forbidden")
    void listRecords_asViewer_returns403() throws Exception {
        mockMvc.perform(get(BASE)
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /records: ANALYST → 200 with records")
    void listRecords_asAnalyst_returns200() throws Exception {
        // Create a record first as admin
        createTestRecord();

        mockMvc.perform(get(BASE)
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /records?type=INCOME: filtered → only INCOME")
    void listRecords_filterByType_returnsFiltered() throws Exception {
        createTestRecord(); // INCOME record

        mockMvc.perform(get(BASE + "?type=INCOME")
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].type", everyItem(is("INCOME"))));
    }

    // ===== UPDATE =====

    @Test
    @DisplayName("PATCH /records/{id}: correct version → 200 updated")
    void updateRecord_correctVersion_returns200() throws Exception {
        MvcResult createResult = createTestRecord();
        String body = createResult.getResponse().getContentAsString();
        String id = objectMapper.readTree(body).at("/data/id").asText();
        int version = objectMapper.readTree(body).at("/data/version").asInt();

        var updateReq = new UpdateRecordRequest(null, new BigDecimal("9999.00"), null, "Updated", version);

        mockMvc.perform(patch(BASE + "/" + id)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(9999.00))
                .andExpect(jsonPath("$.data.notes").value("Updated"));
    }

    @Test
    @DisplayName("PATCH /records/{id}: wrong version → 409 Conflict")
    void updateRecord_wrongVersion_returns409() throws Exception {
        MvcResult createResult = createTestRecord();
        String body = createResult.getResponse().getContentAsString();
        String id = objectMapper.readTree(body).at("/data/id").asText();

        var updateReq = new UpdateRecordRequest(null, new BigDecimal("500.00"), null, null, 99); // wrong version

        mockMvc.perform(patch(BASE + "/" + id)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateReq)))
                .andExpect(status().isConflict());
    }

    // ===== DELETE =====

    @Test
    @DisplayName("DELETE /records/{id}: soft delete → 200, record not visible in GET")
    void deleteRecord_softDelete_returns200AndHidesRecord() throws Exception {
        MvcResult createResult = createTestRecord();
        String body = createResult.getResponse().getContentAsString();
        String id = objectMapper.readTree(body).at("/data/id").asText();

        // Soft delete
        mockMvc.perform(delete(BASE + "/" + id)
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Record should no longer appear in list
        mockMvc.perform(get(BASE)
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").isEmpty());
    }


    private MvcResult createTestRecord() throws Exception {
        var request = new CreateRecordRequest(
                incomeCategory.getId(),
                new BigDecimal("1500.00"),
                TransactionType.INCOME,
                Instant.now().minusSeconds(3600),
                "Test income");
        String setupIp = "10.0.1." + (int)(Math.random() * 255);

        return mockMvc.perform(post(BASE)
                        .with(req -> {
                            req.setRemoteAddr(setupIp);
                            return req;
                        })
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }
}
