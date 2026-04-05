package com.financeapp.finance_backend.auth;

import com.financeapp.finance_backend.BaseIntegrationTest;
import com.financeapp.finance_backend.auth.dto.LoginRequest;
import com.financeapp.finance_backend.auth.dto.RefreshTokenRequest;
import com.financeapp.finance_backend.auth.dto.RegisterRequest;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/auth";

    @Test
    @DisplayName("POST /auth/register: valid data → 201, status INACTIVE")
    void register_validData_returns201() throws Exception {
        var request = new RegisterRequest("newuser@test.com", "Password1!", "New User");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.role").value("VIEWER"))
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));
    }

    @Test
    @DisplayName("POST /auth/register: existing email → 409 Conflict")
    void register_existingEmail_returns409() throws Exception {
        // testAdmin created in BaseIntegrationTest
        var request = new RegisterRequest("admin@test.com", "Password1!", "Duplicate");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/register: weak password → 422")
    void register_weakPassword_returns422() throws Exception {
        var request = new RegisterRequest("weak@test.com", "password", "Weak");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("POST /auth/login: correct credentials → 200 with tokens")
    void login_correctCredentials_returns200WithTokens() throws Exception {
        // Create active user with known password
        createUser("login@test.com", UserRole.ANALYST, UserStatus.ACTIVE);

        var request = new LoginRequest("login@test.com", "Password1!");

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login: wrong password → 401 (generic message)")
    void login_wrongPassword_returns401() throws Exception {
        var request = new LoginRequest("admin@test.com", "Wrongpass1!");

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").doesNotExist()); // no user info leaked
    }

    @Test
    @DisplayName("POST /auth/login: inactive user → 422 Business Rule")
    void login_inactiveUser_returns422() throws Exception {
        createUser("inactive@test.com", UserRole.VIEWER, UserStatus.INACTIVE);

        var request = new LoginRequest("inactive@test.com", "Password1!");

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/login → POST /auth/refresh: rotates token")
    void loginThenRefresh_rotatesToken() throws Exception {
        createUser("refresh@test.com", UserRole.ANALYST, UserStatus.ACTIVE);

        // Login to get tokens
        MvcResult loginResult = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest("refresh@test.com", "Password1!"))))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(body)
                .at("/data/refreshToken").asText();

        // Use refresh token
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/refresh: revoked token → 422")
    void refresh_revokedToken_returns422() throws Exception {
        var request = new RefreshTokenRequest("totally-invalid-token");

        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/logout: authenticated → 200, tokens revoked")
    void logout_authenticated_returns200() throws Exception {
        createUser("logout@test.com", UserRole.ANALYST, UserStatus.ACTIVE);

        // Login to get tokens
        MvcResult loginResult = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest("logout@test.com", "Password1!"))))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).at("/data/accessToken").asText();
        String refreshToken = objectMapper.readTree(body).at("/data/refreshToken").asText();

        // Logout
        mockMvc.perform(post(BASE + "/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk());

        // Refresh with same token should now fail
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("5 consecutive failed logins → account locked")
    void fiveFailedLogins_locksAccount() throws Exception {
        createUser("lockme@test.com", UserRole.VIEWER, UserStatus.ACTIVE);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new LoginRequest("lockme@test.com", "WrongPass1!"))))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt — even with correct password
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest("lockme@test.com", "Password1!"))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
