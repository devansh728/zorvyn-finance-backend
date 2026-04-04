package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must have at least 1 uppercase letter, 1 digit, and 1 special character")
        String password,

        @NotBlank @Size(max = 150) String fullName,

        @NotNull UserRole role
) {}
