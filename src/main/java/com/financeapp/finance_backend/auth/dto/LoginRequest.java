package com.financeapp.finance_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for user login")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "admin@zorvyn.com")
        @NotBlank @Email String email,

        @Schema(description = "Account password", example = "Admin@123")
        @NotBlank String password
) {}
