package com.financeapp.finance_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must have at least 1 uppercase letter, 1 digit, and 1 special character (@$!%*?&)")
        String password,

        @NotBlank @Size(max = 150) String fullName
) {}
