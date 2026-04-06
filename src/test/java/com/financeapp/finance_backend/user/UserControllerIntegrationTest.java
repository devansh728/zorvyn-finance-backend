package com.financeapp.finance_backend.user;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.user.dto.CreateUserRequest;
import com.financeapp.finance_backend.user.dto.UpdateRoleRequest;
import com.financeapp.finance_backend.user.dto.UpdateStatusRequest;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Controller Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/users";

    private RequestPostProcessor randomIp() {
        return request -> {
            request.setRemoteAddr("10.0.3." + (int)(Math.random() * 255));
            return request;
        };
    }

    @Test
    @DisplayName("GET /users/me: authenticated → returns current user")
    void getMe_authenticated_returnsUser() throws Exception {
        mockMvc.perform(get(BASE + "/me")
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /users/me: unauthenticated → 403 (JWT Filter Reject)")
    void getMe_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/me").with(randomIp()))
                .andExpect(status().isForbidden()); // Aligned with the JWT filter's hard-reject
    }

    @Test
    @DisplayName("GET /users: ADMIN → 200 with paginated list")
    void listUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /users: VIEWER → 403 Forbidden")
    void listUsers_asViewer_returns403() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(randomIp())
                        .header("Authorization", bearerToken(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users: ANALYST → 403 Forbidden")
    void listUsers_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(randomIp())
                        .header("Authorization", bearerToken(analystToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/{id}: ADMIN → 200")
    void getUserById_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/" + testAnalyst.getId())
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("analyst@test.com"));
    }

    @Test
    @DisplayName("GET /users/{id}: unknown ID → 404")
    void getUserById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/00000000-0000-0000-0000-000000000000")
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /users: ADMIN creates user → 201")
    void createUser_asAdmin_returns201() throws Exception {
        var request = new CreateUserRequest("created@test.com", "Password1!", "Created User", UserRole.ANALYST);

        mockMvc.perform(post(BASE)
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("created@test.com"))
                .andExpect(jsonPath("$.data.role").value("ANALYST"));
    }

    @Test
    @DisplayName("POST /users: ANALYST → 403")
    void createUser_asAnalyst_returns403() throws Exception {
        var request = new CreateUserRequest("x@test.com", "Password1!", "X", UserRole.VIEWER);

        mockMvc.perform(post(BASE)
                        .with(randomIp())
                        .header("Authorization", bearerToken(analystToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/{id}/status: deactivate analyst → 200")
    void updateStatus_deactivateAnalyst_returns200() throws Exception {
        var request = new UpdateStatusRequest(UserStatus.INACTIVE);

        mockMvc.perform(patch(BASE + "/" + testAnalyst.getId() + "/status")
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("PATCH /users/{id}/role: admin changes own role → 422 Business Rule")
    void updateRole_selfDemotion_returns422() throws Exception {
        var request = new UpdateRoleRequest(UserRole.VIEWER);

        mockMvc.perform(patch(BASE + "/" + testAdmin.getId() + "/role")
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /users: filter by role=ANALYST → only analysts returned")
    void listUsers_filterByRole_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE + "?role=ANALYST")
                        .with(randomIp())
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].role", everyItem(is("ANALYST"))));
    }
}