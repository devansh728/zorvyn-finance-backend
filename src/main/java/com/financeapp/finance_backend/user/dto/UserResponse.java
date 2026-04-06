package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "User profile response")
public record UserResponse(
        @Schema(description = "User identifier", example = "f8bd6314-3ecc-4fd3-bf45-f8be61ad2a36")
        UUID id,

        @Schema(description = "Email address", example = "analyst@zorvyn.com")
        String email,

        @Schema(description = "Full name", example = "Nora Saleh")
        String fullName,

        @Schema(description = "Assigned role", example = "ANALYST", allowableValues = {"VIEWER", "ANALYST", "ADMIN"})
        UserRole role,

        @Schema(description = "Current status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        UserStatus status,

        @Schema(description = "Creation timestamp (UTC)", example = "2026-04-05T09:15:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp (UTC)", example = "2026-04-06T10:45:00Z")
        Instant updatedAt,

        @Schema(description = "Optimistic lock version", example = "3")
        int version
) {}
