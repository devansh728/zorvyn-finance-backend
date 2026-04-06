package com.financeapp.finance_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for user registration")
public record RegisterRequest(
        @Schema(description = "User email address", example = "analyst@zorvyn.com")
        @NotBlank @Email String email,

        @Schema(description = "Account password. Must include uppercase letter, digit, and special character", example = "Secure@123")
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must have at least 1 uppercase letter, 1 digit, and 1 special character (@$!%*?&)")
        String password,

        @Schema(description = "User full name", example = "Aisha Rahman")
        @NotBlank @Size(max = 150) String fullName
) {}
