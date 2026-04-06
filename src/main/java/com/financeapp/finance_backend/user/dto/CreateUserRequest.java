package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to create a new user account")
public record CreateUserRequest(
        @Schema(description = "User email address", example = "viewer@zorvyn.com")
        @NotBlank @Email String email,

        @Schema(description = "Initial account password", example = "Viewer@123")
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must have at least 1 uppercase letter, 1 digit, and 1 special character")
        String password,

        @Schema(description = "Full display name", example = "Omar Nasser")
        @NotBlank @Size(max = 150) String fullName,

        @Schema(description = "Role assigned to the user", example = "VIEWER", allowableValues = {"VIEWER", "ANALYST", "ADMIN"})
        @NotNull UserRole role
) {}
